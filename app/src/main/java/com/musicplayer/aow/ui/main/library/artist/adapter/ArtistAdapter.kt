package com.musicplayer.aow.ui.main.library.artist.adapter

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.musicplayer.aow.R
import com.musicplayer.aow.application.Injection
import com.musicplayer.aow.delegates.data.model.Artists
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.source.AppRepository
import com.musicplayer.aow.ui.main.library.activities.ArtistSongs
import com.musicplayer.aow.utils.images.BitmapDraws
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.onComplete
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Arca on 11/27/2017.
 */
class ArtistAdapter(var context: Context, var activity: Activity, artistList: ArrayList<Artists>?) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    private var view:View? = null
    private val mSubscriptions: CompositeSubscription? = null
    private var mArtistModel = artistList
    var firstAlbumArt = false

    @TargetApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: ArtistViewHolder?, position: Int) {

        var model = mArtistModel?.get(position)
        var numOfSong = model?.numberOfSongs
        var albumArtist = model?.artist_name
        holder?.noOfSongs?.text = if (numOfSong?.toInt()!! <= 1){
            numOfSong.toString().plus(" Track")
        }else{
            numOfSong.toString().plus(" Tracks")
        }
        holder?.albumArtist?.text = albumArtist

//        if (model.artArt != null || firstAlbumArt) {
//            val albumArt = BitmapDraws.createFromPath(model.albumArt)
//            if (albumArt != null) {
//                holder!!.albumArt.setImageDrawable(albumArt)
//            }else{
//                //Drawable Text
//                var generator = ColorGenerator.MATERIAL // or use DEFAULT
//                // generate random color
//                var color1 = generator.randomColor
//                var icon = TextDrawable.builder().buildRect(model?.artist_name!!.substring(0,1), color1)
//                holder!!.albumArt.setImageDrawable(icon)
//            }
//            firstAlbumArt = true
//        }else{
//            //Drawable Text
//            var generator = ColorGenerator.MATERIAL // or use DEFAULT
//            // generate random color
//            var color1 = generator.randomColor
//            var icon = TextDrawable.builder().buildRect(model?.artist_name!!.substring(0,1), color1)
//            holder!!.albumArt.setImageDrawable(icon)
//        }
        doAsync {
            val alb = Injection.provideContext()!!
                    .contentResolver.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(
                            MediaStore.Audio.Albums._ID,
                            MediaStore.Audio.Albums.ALBUM_ART),
                    MediaStore.Audio.Albums.ARTIST + "=?",
                    arrayOf<String>(model?.artist_name!!),
                    null)
            onComplete {
                if (alb.moveToFirst()) {
                    val data = alb.getString(alb.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
                    val albumArt = BitmapDraws.createFromPath(data)
                    if (albumArt != null) {
                        holder!!.albumArt.setImageDrawable(albumArt)
                    }else{
                        holder!!.albumArt.setImageResource(R.drawable.ic_music_cd)
                    }
                }
                alb.close()
            }

        }

//        Glide.with(context)
//                .load(model.albumArt)
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .fitCenter()
//                .error(icon)
//                .into(holder!!.albumArt)

        holder?.cardView?.setOnClickListener {
            val intent = Intent(context, ArtistSongs::class.java)
            intent.putExtra("com.musicplayer.aow.artist.name", albumArtist)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(context, intent, null)
        }

        //to set options
        holder?.option?.setOnClickListener {
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
                var playNext = sheetView!!.find<LinearLayout>(R.id.menu_item_play_next)
                var queue = sheetView!!.find<LinearLayout>(R.id.menu_item_add_to_queue)
                var playlist = sheetView.find<LinearLayout>(R.id.menu_item_add_to_play_list)

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
//                                    if (playlistModelData != null){
//                                        //sort the song list in ascending order
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
//                                    }
                                }
                            })
                    mSubscriptions?.add(subscription)

                    mSelectPlaylistDialog.setContentView(sheetView)
                    mSelectPlaylistDialog.show()
                    mSelectPlaylistDialog.setOnDismissListener {}
                }
            }
        }
    }

    fun swapCursor(artistList: ArrayList<Artists>?): ArrayList<Artists>? {
        if (mArtistModel === artistList) {
            return null
        }
        val oldCursor = mArtistModel
        this.mArtistModel = artistList
        if (artistList != null) {
            this.notifyDataSetChanged()
        }
        return oldCursor
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ArtistViewHolder {
        view = LayoutInflater.from(parent!!.context).inflate(R.layout.artist_card_view,parent,false)
        return ArtistViewHolder(view!!)
    }

    //we get the count of the list
    override fun getItemCount(): Int {
        return mArtistModel?.size!!
    }

    class ArtistViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var noOfSongs: TextView = itemView.find<TextView>(R.id.artist_no_songs)
        var albumArtist: TextView = itemView.find<TextView>(R.id.artist_name)
        var albumArt: ImageView = itemView.find<ImageView>(R.id.artist_album_art)
        var cardView: CardView = itemView.find<CardView>(R.id.card_view_container_artist)
        var option: AppCompatImageView = itemView.find<AppCompatImageView>(R.id.item_button_action)
        var view: View = itemView
    }

}