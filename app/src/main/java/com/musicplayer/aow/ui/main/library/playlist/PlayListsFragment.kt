package com.musicplayer.aow.ui.main.library.playlist


import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.database.DatabaseUtils
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.musicplayer.aow.R
import com.musicplayer.aow.bus.RxBus
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.source.AppRepository
import com.musicplayer.aow.delegates.event.ReloadEvent
import com.musicplayer.aow.ui.base.BaseFragment
import com.musicplayer.aow.utils.DeviceUtils
import com.musicplayer.aow.utils.layout.PreCachingLayoutManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import rx.Subscriber
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription




class PlayListsFragment : BaseFragment(){

    internal var progress_bar: ProgressBar? = null
    internal var recycler_view: RecyclerView? = null
    private val mSubscriptions: CompositeSubscription? = null
    internal var btn_create_playlist: Button? = null

    var playListAdapter:PlayListAdapter? = null
    var playList:List<PlayList>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_play_lists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress_bar = view.find(R.id.progress_bar)
        recycler_view = view.find(R.id.recycler_playlist_views)
        btn_create_playlist = view.find<Button>(R.id.create_playlist)
        //create playlist
        btn_create_playlist!!.setOnClickListener {
            showChangeLangDialog()
        }

        loadPlayLists()
        //local()
    }

    override fun onResume() {
        super.onResume()
        //local()
    }


    private fun showChangeLangDialog() {
        try {
            val dialogBuilder = AlertDialog.Builder(context!!, android.R.style.Theme_Material_Light_Dialog)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.custom_dialog_input, null)
            dialogBuilder.setView(dialogView)

            val edt = dialogView.find<EditText>(R.id.edit1)
            edt.setText("New playlist")

            dialogBuilder.setTitle("Playlist name").setIcon(R.drawable.ic_play_now_rename)
            dialogBuilder.setPositiveButton("Save", DialogInterface.OnClickListener { dialog, which ->
                //do something with edt.getText().toString();
                if (edt.text.toString() != null) {
                    val newPlayList = PlayList()
                    newPlayList.name = edt.text.toString()
                    createPlayList(newPlayList)
                }
            })
            dialogBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> })
            dialogBuilder.create().show()
        }catch (e: NullPointerException){
            //
        }
    }

    fun local(){
        var cursor = context!!.contentResolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null)
        var id = 0
        if (cursor.moveToFirst()) {
            if (cursor.getColumnIndex("_id") !== -1) {
                id = cursor.getInt(cursor.getColumnIndex("_id"))
            }
        }
        Log.d("playlist", DatabaseUtils.dumpCursorToString(cursor))
        cursor.close()
        if (id != 0) {
            cursor = context!!.contentResolver.query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", id.toLong()), null, null, null, null)
            Log.d("playlist", DatabaseUtils.dumpCursorToString(cursor))
            cursor.close()
        }
    }

    fun getSongCountForPlaylist(context: Context, playlistId: Long): Int {
        var c = context.contentResolver.query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                arrayOf(BaseColumns._ID), null, null, null)

        if (c != null) {
            var count = 0
            if (c.moveToFirst()) {
                count = c.count
            }
            c.close()
            c = null
            return count
        }

        return 0
    }


    fun getPlaylistName(mContext: Context, playlist_id: Long): String {
        var where = BaseColumns._ID + "=" + playlist_id
        var cols = arrayOf(MediaStore.Audio.PlaylistsColumns.NAME)
        var uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        var cursor = mContext.contentResolver.query(uri, cols, where, null, null)
        if (cursor == null){
            return ""
        }
        if (cursor.count <= 0)
            return ""
        cursor.moveToFirst()
        var name = cursor.getString(0)
        cursor.close()
        return name
    }

    fun createPlaylist(context: Context, name: String): Long {
        if (name.isNotEmpty()) {
            val resolver = context.contentResolver
            val cols = arrayOf(
                MediaStore.Audio.PlaylistsColumns.NAME
                )
            val whereclause = MediaStore.Audio.PlaylistsColumns.NAME + " = '" + name + "'"
            val cur = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols, whereclause,
                    null, null)
            if (cur.count <= 0) {
                var values = ContentValues(1)
                values.put(MediaStore.Audio.PlaylistsColumns.NAME, name)
                var uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values)
                cur.close()
                return uri.lastPathSegment.toLong()
            }
            cur.close()
            return -1
        }
        return -1
    }

    fun getFavoritesId(context: Context): Long {
        var favorites_id = -1L
        val favorites_where = MediaStore.Audio.PlaylistsColumns.NAME + "='" + "Favorites" + "'"
        val favorites_cols = arrayOf(
            BaseColumns._ID
        )
        val favorites_uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver.query(favorites_uri, favorites_cols, favorites_where, null, null)
        if (cursor.count <= 0) {
            favorites_id = createPlaylist(context, "Favorites")
        } else {
            cursor.moveToFirst()
            favorites_id = cursor.getLong(0)
            cursor.close()
        }
        return favorites_id
    }
    


    fun loadData(playlist: List<PlayList>?){
        var playlistModelData = playlist
        //get audio from shearPref
        if (playlistModelData == null){
            progress_bar!!.visibility = View.INVISIBLE
        }else {
            progress_bar!!.visibility = View.INVISIBLE
            //sort the song list in ascending order
            playList = playlistModelData.sortedWith(compareBy({ (it.name)!!.toLowerCase() }))
            //Save to database
            playListAdapter = PlayListAdapter(context!!.applicationContext, playList!!, this.layoutInflater)
            //Setup layout manager
            val layoutManager = PreCachingLayoutManager(activity!!)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(activity!!))
            recycler_view!!.setHasFixedSize(true)
            recycler_view!!.layoutManager = layoutManager
            recycler_view!!.adapter = playListAdapter
        }
    }

    fun loadPlayLists(){
        val subscription = AppRepository().playLists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<List<PlayList>>() {
                    override fun onStart() {}

                    override fun onCompleted() {}

                    override fun onError(e: Throwable) {}

                    override fun onNext(playLists: List<PlayList>) {
                        loadData(playLists)
                    }
                })
        mSubscriptions?.add(subscription)
    }

    private fun createPlayList(playList: PlayList) {
        val subscription = AppRepository().create(playList).subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(object : Subscriber<PlayList>() {
                    override fun onStart() {}

                    override fun onCompleted() {}

                    override fun onError(e: Throwable) {}

                    override fun onNext(result: PlayList) {
                        loadPlayLists()
                    }
                })
        mSubscriptions?.add(subscription)
    }

    // RXBus Events
    override fun subscribeEvents(): Subscription {
        return RxBus.instance?.toObservable()
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.doOnNext({ o ->
                    when(o){
                        is PlayListAction -> {
                            loadPlayLists()
                        }
                        is ReloadEvent -> {
                            doAsync {
                                loadPlayLists()
                            }
                        }
                    }
                })?.subscribe(RxBus.defaultSubscriber())!!
    }

}
