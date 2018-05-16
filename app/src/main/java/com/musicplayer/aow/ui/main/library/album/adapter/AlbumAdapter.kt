package com.musicplayer.aow.ui.main.library.album.adapter

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.musicplayer.aow.R
import com.musicplayer.aow.R.id.menu_item_add_to_queue
import com.musicplayer.aow.R.id.menu_item_play_next
import com.musicplayer.aow.delegates.data.model.Album
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.source.AppRepository
import com.musicplayer.aow.ui.main.library.activities.AlbumSongs
import com.musicplayer.aow.utils.CursorDB
import com.musicplayer.aow.utils.images.BitmapDraws
import org.jetbrains.anko.find
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*


/**
 * Created by Arca on 11/20/2017.
 */
class AlbumAdapter(var context: Context, var activity: Activity, albumList: ArrayList<Album>?) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    private var view:View? = null
    private val mSubscriptions: CompositeSubscription? = null
    var mAlbumModel = albumList

    @TargetApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: AlbumViewHolder?, position: Int) {
        var model = mAlbumModel?.get(position)
        holder!!.albumName.text = model?.albumName
        holder.albumArtist.text = model?.artist

        if (model?.albumArt != null || model?.albumArt != "null") {
            val albumArt = BitmapDraws.createFromPath(model?.albumArt)
            if (albumArt != null) {
                holder.albumArt.setImageDrawable(albumArt)
            }else{
                holder.albumArt.setImageResource(R.drawable.ic_music_cd)
            }
        } else{
            holder.albumArt.setImageResource(R.drawable.ic_music_cd)
        }

        holder.cardView.setOnClickListener {
            Log.e(this.javaClass.name, "album Id is ${model?.album_id}")
            val intent = Intent(context, AlbumSongs::class.java).apply {
                putExtra("com.musicplayer.aow.album.id", model?.album_id)
                putExtra("com.musicplayer.aow.album.name", model?.albumName)
                putExtra("com.musicplayer.aow.album.artist", model?.artist)
                putExtra("com.musicplayer.aow.album.album_art", model?.albumArt)
                putExtra("com.musicplayer.aow.album.numberOfSongs", model?.numberOfSongs)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(context, intent, null)
        }

        //to set options
        holder.option.setOnClickListener {
            if (holder.view != null) {
                var context = holder.view.context
                val mBottomSheetDialog = BottomSheetDialog(context)
                val sheetView =  LayoutInflater.from(context).inflate(R.layout.bottom_sheet_modal_dialog_album, null)
                mBottomSheetDialog.setContentView(sheetView)
                mBottomSheetDialog.show()
                mBottomSheetDialog.setOnDismissListener {
                    //perform action on close
                }

                var play = sheetView!!.find<LinearLayout>(R.id.menu_item_play_now)
                var playNext = sheetView!!.find<LinearLayout>(menu_item_play_next)
                var queue = sheetView!!.find<LinearLayout>(menu_item_add_to_queue)
                var playlist = sheetView.find<LinearLayout>(R.id.menu_item_add_to_play_list)
                val delete = sheetView.find<LinearLayout>(R.id.menu_item_delete)

                play.setOnClickListener {
                    //RxBus.instance!!.post(PlayListNowEvent(model.PlayList, 0))
                    mBottomSheetDialog.dismiss()
                }

                playNext.setOnClickListener {
                    //Player.instance!!.insertnext(Player.instance!!.mPlayList!!.playingIndex,model.PlayList.songs as ArrayList<Song>)
                    mBottomSheetDialog.dismiss()
                }

                queue.setOnClickListener {
                    //Player.instance!!.insertnext(Player.instance!!.mPlayList!!.numOfSongs,model.PlayList.songs as ArrayList<Song>)
                    mBottomSheetDialog.dismiss()
                }

                playlist.setOnClickListener {
                    mBottomSheetDialog.dismiss()
                    //Dialog with ListView
                    var context = view!!.context
                    val mSelectPlaylistDialog = BottomSheetDialog(context)
                    val sheetView =  LayoutInflater.from(context).inflate(R.layout.custom_dialog_select_playlist, null)
                    var mylist = sheetView.find<RecyclerView>(R.id.recycler_playlist_views)

                    //load data
                    val subscription = AppRepository().playLists()
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
//                                        var playList = playlistModelData.sortedWith(compareBy({ (it.name)!!.toLowerCase() }))
//                                        //Save to database
//                                        var playListAdapter = PlaylistDialogSLAdapter(activity, playList, model.PlayList, mSelectPlaylistDialog)
//                                        //Setup layout manager
//                                        val layoutManager = PreCachingLayoutManager(activity)
//                                        layoutManager.orientation = LinearLayoutManager.VERTICAL
//                                        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(activity))
//                                        mylist.setHasFixedSize(true)
//                                        mylist.layoutManager = layoutManager
//                                        mylist.adapter = playListAdapter
                                    }
                                }
                            })
                    mSubscriptions?.add(subscription)

                    mSelectPlaylistDialog.setContentView(sheetView)
                    mSelectPlaylistDialog.show()
                    mSelectPlaylistDialog.setOnDismissListener {}
                }

                delete.setOnClickListener {
                    val albumSongsCursor = CursorDB().getAlbumSongs(context, model?.album_id!!)
                    if (albumSongsCursor != null){
                        while (albumSongsCursor.moveToNext()){
                            CursorDB().deletMusic(albumSongsCursor, context)
                        }
                        albumSongsCursor.close()
                    }
                    mBottomSheetDialog.dismiss()
                }
            }
        }
    }

    fun swapCursor(albumList: ArrayList<Album>?): ArrayList<Album>? {
        if (mAlbumModel === albumList) {
            return null
        }
        val oldCursor = mAlbumModel
        this.mAlbumModel = albumList
        if (albumList != null) {
            this.notifyDataSetChanged()
        }
        return oldCursor
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AlbumViewHolder {
        view = LayoutInflater.from(parent!!.context).inflate(R.layout.container_fish,parent,false)
        return AlbumViewHolder(view!!)
    }

    //we get the count of the list
    override fun getItemCount(): Int {
        return mAlbumModel?.size!!
    }

    class AlbumViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var albumName: TextView = itemView.find(R.id.cardname)
        var albumArtist: TextView = itemView.find(R.id.cardart)
        var albumArt: ImageView = itemView.find(R.id.ivFish)
        var cardView: LinearLayout = itemView.find(R.id.card_view_container)
        var option: AppCompatImageView = itemView.find(R.id.item_button_action)
        var view: View = itemView
    }

}