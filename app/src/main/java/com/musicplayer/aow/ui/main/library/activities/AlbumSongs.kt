package com.musicplayer.aow.ui.main.library.activities

import android.annotation.SuppressLint
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.musicplayer.aow.R
import com.musicplayer.aow.bus.RxBus
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.model.Song
import com.musicplayer.aow.delegates.event.PlayAlbumNowEvent
import com.musicplayer.aow.ui.main.library.activities.albumsonglist.AlbumSongListAdapter
import com.musicplayer.aow.utils.CursorDB
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.onComplete
import java.util.*


/**
 * Created by Arca on 12/1/2017.
 */
class AlbumSongs : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    private var ALBUM_ID: String? = "0"
    private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private var WHERE = (MediaStore.Audio.Media.ALBUM_ID + "=$ALBUM_ID AND " + MediaStore.Audio.Media.SIZE + ">0" )
    private val ORDER_BY = MediaStore.Audio.Media.TITLE + " ASC"
    private val PROJECTIONS = arrayOf(
            MediaStore.Audio.Media.DATA, // the real path
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE)
    private var songs: PlayList = PlayList()

    var albumModelData: ArrayList<Song> = ArrayList()
    private var songsList:List<Song>? = null
    private var albumArt: ImageView? = null
    private var albumArtMain: ImageView? = null
    private var albumArtName: TextView? = null
    var playAlbumFab: Button? = null

    private var numberOfSongs: TextView? = null
    private var mAlbumList: RecyclerView? = null
    private var adapter: AlbumSongListAdapter? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_song_list)
        val toolbarNavigation = findViewById<AppCompatImageView>(R.id.toolbar_album_song_list)

        // Set the padding to match the Status Bar height
        toolbarNavigation.setOnClickListener {
            finish()
        }

        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)

        numberOfSongs = find<TextView>(R.id.numbers_of_songs)
        albumArtMain = findViewById<ImageView>(R.id.image_view_album_art_main)
        albumArtName = findViewById<TextView>(R.id.image_view_album_name_main)
        mAlbumList = findViewById<RecyclerView>(R.id.album_songs_recycler_views)
        playAlbumFab = findViewById<Button>(R.id.fab_play_album)
        //paying audio from other apps
        val intent = intent
        if (intent != null) {
            // To get the data use
            val id = intent.getStringExtra("com.musicplayer.aow.album.id")
            val name = intent.getStringExtra("com.musicplayer.aow.album.name")
            val artist = intent.getStringExtra("com.musicplayer.aow.album.artist")
            val album_art = intent.getStringExtra("com.musicplayer.aow.album.album_art")
            val album_numberOfSongs = intent.getStringExtra("com.musicplayer.aow.album.numberOfSongs")
            if (id != null) {
                val bundle = Bundle()
                bundle.putString("_id", id)
                supportLoaderManager.initLoader(0, bundle, this)
                albumArtName!!.text = name
                numberOfSongs!!.text = album_numberOfSongs
                collapsingToolbarLayout.title = name
                collapsingToolbarLayout.setContentScrimColor(Color.WHITE)
                //Album art
                val Art = Drawable.createFromPath(album_art)
                if(Art != null) {
                    albumArtMain!!.setImageDrawable(Art)
                }
                data()
            }
        }
    }

    fun data(){

        //sort the song list in ascending order
        songsList = albumModelData.sortedWith(compareBy({ (it.title)!!.toLowerCase() }))
        //play as playlist when album art is clicked
        playAlbumFab!!.setOnClickListener {
            RxBus.instance!!.post(PlayAlbumNowEvent(songsList!!))
        }

        var sizeOfSongs = songs.songs.size
        if(sizeOfSongs > 1){
            numberOfSongs!!.text = sizeOfSongs.toString() + getString(R.string.tracks)
        }else{
            numberOfSongs!!.text = sizeOfSongs.toString() + getString(R.string.track)
        }

        adapter = AlbumSongListAdapter(this, songs, this)
        mAlbumList!!.adapter = adapter
        var layoutManager = LinearLayoutManager(this)
        mAlbumList!!.setHasFixedSize(true)
        mAlbumList!!.layoutManager = layoutManager
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        ALBUM_ID = args?.getString("_id")
        WHERE = (MediaStore.Audio.Media.ALBUM_ID + "=$ALBUM_ID AND " + MediaStore.Audio.Media.SIZE + ">0" )
        return CursorLoader(applicationContext, MEDIA_URI,
                PROJECTIONS, WHERE, null,
                ORDER_BY)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        songs = PlayList()
        if (data != null) {
            doAsync {
                while (data.moveToNext()) {
                    songs.addSong(CursorDB().cursorToMusic(data))
                }
                onComplete {
                    adapter?.swapCursor(songs.songs as ArrayList<Song>)
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {

    }
}