package com.musicplayer.aow.ui.main.library.activities.albumsonglist

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.musicplayer.aow.R
import com.musicplayer.aow.bus.RxBus
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.model.Song
import com.musicplayer.aow.delegates.data.model.TempSongs
import com.musicplayer.aow.delegates.data.source.AppRepository
import com.musicplayer.aow.delegates.event.PlayListNowEvent
import com.musicplayer.aow.delegates.event.PlaySongEvent
import com.musicplayer.aow.delegates.event.ReloadEvent
import com.musicplayer.aow.delegates.player.Player
import com.musicplayer.aow.ui.main.library.activities.ArtistSongs
import com.musicplayer.aow.ui.main.library.songs.dialog.adapter.PlaylistDialogAdapter
import com.musicplayer.aow.utils.CursorDB
import com.musicplayer.aow.utils.DeviceUtils
import com.musicplayer.aow.utils.TimeUtils.formatDuration
import com.musicplayer.aow.utils.layout.PreCachingLayoutManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.onComplete
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription


/**
 * Created by Arca on 12/1/2017.
 */
class AlbumSongListAdapter(context: Context, song: PlayList?, activity: Activity):RecyclerView.Adapter<AlbumSongListAdapter.SongListViewHolder>() {

    private var activity = activity
    private var view:View? = null
    private val mRepository: AppRepository? = AppRepository.instance
    private val mSubscriptions: CompositeSubscription? = null
    val TAG = this.javaClass.name
    var context = context
    private var mSongModel: ArrayList<Song>? = song?.songs as ArrayList<Song>

    @TargetApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: SongListViewHolder?, position: Int) {
        var model = mSongModel?.get(position)
        var songName = model?.title
        var songDuration = formatDuration(model?.duration!!)
        var songArtist = model?.artist
        holder!!.songTV.text = songName
        holder.songArtist.text = songArtist

        //implementation of item click
        holder.mListItem.setOnClickListener {
            RxBus.instance!!.post(PlaySongEvent(mSongModel?.get(position)!!))
        }

        //here we set item click for songs
        //to set options
        holder.option.setOnClickListener {
            if (view != null) {
                var context = view!!.context
                val mBottomSheetDialog = BottomSheetDialog(context)
                val sheetView =  LayoutInflater.from(context).inflate(R.layout.bottom_sheet_modal_dialog_all_music, null)
                mBottomSheetDialog.setContentView(sheetView)
                mBottomSheetDialog.show()
                mBottomSheetDialog.setOnDismissListener {
                    //perform action on close
                }

                var play = sheetView!!.find<LinearLayout>(R.id.menu_item_play_now)
                var playNext = sheetView.find<LinearLayout>(R.id.menu_item_play_next)
                var addToQueue = sheetView.find<LinearLayout>(R.id.menu_item_add_to_queue)
                var delete = sheetView.find<LinearLayout>(R.id.menu_item_delete)
                var album = sheetView.find<LinearLayout>(R.id.menu_item_go_to_album)
                var artist = sheetView.find<LinearLayout>(R.id.menu_item_go_to_artist)
                var playlist = sheetView.find<LinearLayout>(R.id.menu_item_add_to_play_list)

                play.setOnClickListener {
                    //Update UI
                    RxBus.instance!!.post(PlayListNowEvent(PlayList(mSongModel), position))
                    mBottomSheetDialog.dismiss()
                }
                //play next
                playNext.setOnClickListener {
                    var playingIndex = Player.instance!!.mPlayList!!.playingIndex
                    Player.instance!!.insertnext(playingIndex,model)
                    mBottomSheetDialog.dismiss()
                }
                //add to now playing
                addToQueue.setOnClickListener {
                    Player.instance!!.insertnext(Player.instance!!.mPlayList!!.numOfSongs,model)
                    mBottomSheetDialog.dismiss()
                }

                album.visibility = View.GONE

                artist.setOnClickListener {
                    val intent = Intent(context, ArtistSongs::class.java)
                    intent.putExtra("com.musicplayer.aow.artist.name", model.artist)
                    ContextCompat.startActivity(context, intent, null)
                    mBottomSheetDialog.dismiss()
                }
                //Add to Playlist Operation
                playlist.setOnClickListener {
                    mBottomSheetDialog.dismiss()
                    //Dialog with ListView
                    var context = view!!.context
                    val mSelectPlaylistDialog = BottomSheetDialog(context)
                    val sheetView =  LayoutInflater.from(context).inflate(R.layout.custom_dialog_select_playlist, null)
                    var mylist = sheetView.find<RecyclerView>(R.id.recycler_playlist_views)

                    //load data
                    val subscription = mRepository!!.playLists()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : Subscriber<List<PlayList>>() {
                                override fun onStart() {}

                                override fun onCompleted() {}

                                override fun onError(e: Throwable) {}

                                override fun onNext(playLists: List<PlayList>) {
                                    //recycler adapter
                                    var playlistModelData = playLists
                                    //get audio from shearPref
                                    if (playlistModelData != null){
                                        //sort the song list in ascending order
                                        var playList = playlistModelData.sortedWith(compareBy({ (it.name)!!.toLowerCase() }))
                                        //Save to database
                                        var playListAdapter = PlaylistDialogAdapter(activity, playList, model, mSelectPlaylistDialog)
                                        //Setup layout manager
                                        val layoutManager = PreCachingLayoutManager(activity)
                                        layoutManager.orientation = LinearLayoutManager.VERTICAL
                                        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(activity))
                                        mylist.setHasFixedSize(true)
                                        mylist.layoutManager = layoutManager
                                        mylist.adapter = playListAdapter
                                    }
                                }
                            })
                    mSubscriptions?.add(subscription)

                    mSelectPlaylistDialog.setContentView(sheetView)
                    mSelectPlaylistDialog.show()
                    mSelectPlaylistDialog.setOnDismissListener {}
                }
                //Delete Operation
                delete.setOnClickListener{
                    CursorDB().deleteMusic(context, model.path)
                    mBottomSheetDialog.dismiss()
                }
            }
        }

    }

    fun swapCursor(playList: ArrayList<Song>?): ArrayList<Song>? {
        if (mSongModel === playList) {
            return null
        }
        val oldCursor = mSongModel
        this.mSongModel = playList
        if (playList != null) {
            this.notifyDataSetChanged()
        }
        return oldCursor
    }

    private fun removeAt(position: Int, song: Song) {
        mSongModel?.remove(song)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, mSongModel?.size!!)
        refreshDatabase(song)
    }

    private fun refreshDatabase(song: Song){
        doAsync {
            TempSongs.instance!!.setSongs()
            onComplete {
                updateAllSongsPlayList(PlayList(TempSongs.instance!!.songs), song)
            }
        }
    }

    private fun updateAllSongsPlayList(playList: PlayList, song: Song) {
        val subscription = mRepository!!.setInitAllSongs(playList).subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(object : Subscriber<PlayList>() {
                    override fun onStart() {}

                    override fun onCompleted() {
                        RxBus.instance!!.post(ReloadEvent(song))
                    }

                    override fun onError(e: Throwable) {}

                    override fun onNext(result: PlayList) {
                    }
                })
        mSubscriptions?.add(subscription)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SongListViewHolder {
        view = LayoutInflater.from(parent!!.context).inflate(R.layout.item_track, parent, false)
        return SongListViewHolder(view!!)
    }

    //we get the count of the list
    override fun getItemCount(): Int {
        return mSongModel?.size!!
    }

    class SongListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var songTV: TextView = itemView.find<TextView>(R.id.text_view_name)
        var songArtist: TextView = itemView.find<TextView>(R.id.text_view_artist)
        var option: AppCompatImageView = itemView.find<AppCompatImageView>(R.id.item_button_action)
        var mListItem: RelativeLayout = itemView.find<RelativeLayout>(R.id.song_list_item)
    }
}