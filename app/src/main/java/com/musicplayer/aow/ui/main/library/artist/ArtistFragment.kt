package com.musicplayer.aow.ui.main.library.artist


import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.musicplayer.aow.R
import com.musicplayer.aow.delegates.data.model.Artists
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.model.Song
import com.musicplayer.aow.delegates.data.source.AppRepository
import com.musicplayer.aow.delegates.objects.ArtistLists
import com.musicplayer.aow.ui.base.BaseFragment
import com.musicplayer.aow.ui.main.library.artist.adapter.ArtistAdapter
import com.musicplayer.aow.utils.CursorDB
import kotlinx.android.synthetic.main.fragment_artist.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.onComplete
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*

class ArtistFragment : BaseFragment(), LoaderManager.LoaderCallbacks<Cursor> {

    private val MEDIA_URI = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
    private val WHERE = null
    private val ORDER_BY = MediaStore.Audio.Artists.ARTIST + " ASC"
    private val PROJECTIONS = arrayOf(
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
    private var adapter: ArtistAdapter? = null
    var mModelData:ArrayList<Artists> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_artist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loaderManager.initLoader(0, null, this)
        data()
    }

    fun data(){
        adapter = ArtistAdapter(context!!.applicationContext, activity!! , mModelData)
        artist_recycler_views.adapter = adapter
        artist_recycler_views.layoutManager = GridLayoutManager(activity, 3)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(context!!, MEDIA_URI,
                PROJECTIONS, WHERE, null,
                ORDER_BY)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        mModelData = ArrayList()
        Log.e(this.javaClass.name, "${mModelData.size} of artists")
        if (data != null) {
            doAsync {
                while (data.moveToNext()) {
                    mModelData.add(CursorDB().cursorToArtistsList(data))
                }
                onComplete {
                    adapter?.swapCursor(mModelData)
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        adapter?.swapCursor(null)
    }

}

