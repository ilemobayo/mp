package com.musicplayer.aow.ui.main.library.home

import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.musicplayer.aow.R
import com.musicplayer.aow.application.Injection
import com.musicplayer.aow.ui.base.BaseFragment
import com.musicplayer.aow.ui.browse.adapter.RecyclerViewAdapter
import com.musicplayer.aow.ui.browse.model.sectiondatamodel.SectionDataModel
import com.musicplayer.aow.ui.browse.model.singleitemmodel.SingleItemModel
import com.musicplayer.aow.ui.main.library.home.deezer.OnlinePlayList
import com.rxandroidnetworking.RxAndroidNetworking
import kotlinx.android.synthetic.main.fragment_home_library.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.json.JSONException
import org.json.JSONObject
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.*
import java.util.*


/**
 * Created by Arca on 1/25/2018.
 */
class DiscoveryFragment : BaseFragment() {

    var allSampleData: ArrayList<SectionDataModel>? = null


    private fun createMainSectionData(section: ArrayList<OnlinePlayList>) {
        var discover = section
        discover.forEach {
            val url = it.tracklist
            var section__ = ArrayList<SingleItemModel>()
            //prepare the Request
            RxAndroidNetworking.get(url)
                    .build()
                    .jsonObjectObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object: rx.Observer<JSONObject>{
                        override fun onError(e: Throwable?) {
                            doAsync {
                                var data = readFromFile(File(Injection.provideContext()!!.externalCacheDir,
                                        "section_a_inner_${it.id}.aow"))!!.getJSONArray("data")
                                for (i in 0..(3)) {
                                    var item = data.getJSONObject(i)
                                    var playlistSection = SingleItemModel()
                                    playlistSection.name = item.getString("title")
                                    playlistSection.artist = item.getJSONObject("artist").getString("name")
                                    playlistSection.link = item.getString("preview")
                                    playlistSection.url = item.getJSONObject("artist").getString("picture_xl")
                                    section__.add(playlistSection)
                                }
                                getMainSectionChildren(it, section__)

                            }
                        }

                        override fun onNext(t: JSONObject?) {
                            //var data = t!!.getJSONArray("data")
                            saveToFile(t!!, File(Injection.provideContext()!!.externalCacheDir,
                                    "section_a_inner_${it.id}.aow"))
                            doAsync {
                                var data = readFromFile(File(Injection.provideContext()!!.externalCacheDir,
                                        "section_a_inner_${it.id}.aow"))!!.getJSONArray("data")
                                for(i in 0..(3)){
                                    var item = data.getJSONObject(i)
                                    var playlistSection = SingleItemModel()
                                    playlistSection.name = item.getString("title")
                                    playlistSection.artist = item.getJSONObject("artist").getString("name")
                                    playlistSection.link = item.getString("preview")
                                    playlistSection.url = item.getJSONObject("artist").getString("picture_xl")
                                    section__.add(playlistSection)
                                }
                                getMainSectionChildren(it, section__)
                            }
                        }

                        override fun onCompleted() {
                            //
                        }
                    } )

        }
    }

    fun getMainSectionChildren(sectionMain:OnlinePlayList, sectionChild: ArrayList<SingleItemModel>){
        val dm = SectionDataModel()
        dm.headerTitle = sectionMain.title

        //Set Data for Each section with playlists, albums e.t.c.
        val singleItem = ArrayList<SingleItemModel>()
        sectionChild.forEach {
            singleItem.add(it)
        }

        dm.allItemsInSection = singleItem
        allSampleData?.add(dm)

        val adapter = RecyclerViewAdapter(context!!.applicationContext, allSampleData)
        music_update_recycler_view_body!!.adapter = adapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_home_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        podcast.setOnClickListener {
//            var intent = Intent(context!!.applicationContext, PodcastActivity::class.java)
//            startActivity(intent)
//        }


        // Setup and Handover data to recyclerview
        music_update_recycler_view_body!!.layoutManager = LinearLayoutManager(context!!.applicationContext, LinearLayoutManager.VERTICAL, false)
        allSampleData = ArrayList()

        //call section API deezer
        callPlaylist()
        doAsync {
            //localOne()
            //callPlaylist()
        }
    }



    fun localOne(){
        var section__ = ArrayList<OnlinePlayList>()
        doAsync {
            var data = readFromFile(File(Environment.getExternalStorageDirectory(), "musixplay" + "/cache/section_a.aow"))!!.getJSONArray("data")
            onComplete {
                for (i in 0..(data.length() - 1)) {
                    var item = data.getJSONObject(i)
                    var playlistSection = OnlinePlayList()
                    playlistSection.id = item.getInt("id")
                    playlistSection.title = item.getString("title")
                    playlistSection.public = item.getBoolean("public")
                    playlistSection.nbTracks = item.getInt("nb_tracks")
                    playlistSection.link = item.getString("link")
                    playlistSection.picture = item.getString("picture")
                    playlistSection.pictureBig = item.getString("picture_big")
                    playlistSection.checksum = item.getString("checksum")
                    playlistSection.tracklist = item.getString("tracklist")
                    section__.add(playlistSection)
                }
                localTwo(section__)
                Log.e("Local 1", section__.size.toString())
            }
        }
    }

    fun localTwo(section: ArrayList<OnlinePlayList>){
        var discover = section
        discover.forEach {
            val url = it.tracklist
            var section__ = ArrayList<SingleItemModel>()
            //prepare the Request

            doAsync {
                var data = readFromFile(File(Environment.getExternalStorageDirectory(),
                        "musixplay/cache/section_a_inner_${it.id}.aow"))!!.getJSONArray("data")
                for (i in 0..(3)) {
                    var item = data.getJSONObject(i)
                    var playlistSection = SingleItemModel()
                    playlistSection.name = item.getString("title")
                    playlistSection.artist = item.getJSONObject("artist").getString("name")
                    playlistSection.link = item.getString("preview")
                    playlistSection.url = item.getJSONObject("artist").getString("picture_xl")
                    section__.add(playlistSection)
                }
                getMainSectionChildren(it, section__)

            }

        }
    }



    private fun callPlaylist(){
        val url = "https://api.deezer.com/chart/0/playlists"
        var section__ = ArrayList<OnlinePlayList>()
        //prepare the Request

        RxAndroidNetworking.get(url)
                 .build()
                 .jsonObjectObservable
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(object: rx.Observer<JSONObject> {
                     override fun onError(e: Throwable?) {
                         doAsync {
                             var data = readFromFile(File(Injection.provideContext()!!.externalCacheDir, "section_a.aow"))!!.getJSONArray("data")
                             onComplete {
                                 for (i in 0..(data.length() - 1)) {
                                     var item = data.getJSONObject(i)
                                     var playlistSection = OnlinePlayList()
                                     playlistSection.id = item.getInt("id")
                                     playlistSection.title = item.getString("title")
                                     playlistSection.public = item.getBoolean("public")
                                     playlistSection.nbTracks = item.getInt("nb_tracks")
                                     playlistSection.link = item.getString("link")
                                     playlistSection.picture = item.getString("picture")
                                     playlistSection.pictureBig = item.getString("picture_big")
                                     playlistSection.checksum = item.getString("checksum")
                                     playlistSection.tracklist = item.getString("tracklist")
                                     section__.add(playlistSection)
                                 }
                                 try {
                                     createMainSectionData(section__)
                                 }catch (e: JSONException){
                                     //
                                 }
                             }
                         }
                     }

                     override fun onNext(t: JSONObject?) {
                         // display response
                         var data = t!!.getJSONArray("data")
                         doAsync {
                             //save to file
                             saveToFile(t, File(Injection.provideContext()!!.externalCacheDir, "section_a.aow"))
                             onComplete {
                                 val data = readFromFile(File(Injection.provideContext()!!.externalCacheDir, "section_a.aow"))?.getJSONArray("data")
                                 for (i in 0..(data?.length()!!.minus(1))) {
                                     var item = data.getJSONObject(i)
                                     var playlistSection = OnlinePlayList()
                                     playlistSection.id = item.getInt("id")
                                     playlistSection.title = item.getString("title")
                                     playlistSection.public = item.getBoolean("public")
                                     playlistSection.nbTracks = item.getInt("nb_tracks")
                                     playlistSection.link = item.getString("link")
                                     playlistSection.picture = item.getString("picture")
                                     playlistSection.pictureBig = item.getString("picture_big")
                                     playlistSection.checksum = item.getString("checksum")
                                     playlistSection.tracklist = item.getString("tracklist")
                                     section__.add(playlistSection)
                                 }
                                 try {
                                     createMainSectionData(section__)
                                 }catch (e: JSONException){
                                     //
                                 }
                             }
                         }
                     }

                     override fun onCompleted() {
                     }
                 })
    }

    private fun saveToFile(obj: JSONObject, location_file: File){
        try {
            val file = location_file
            if (file.exists()) {
                file.bufferedWriter().use { it.write(obj.toString()) }
            }else{
                val out = FileOutputStream(file.absolutePath)
                out.bufferedWriter().use { it.write(obj.toString()) }
                out.close()
            }
        } catch (e: IOException) {
            Log.e(this.javaClass.name, "${e}")
            e.printStackTrace()
        }
    }

    private fun readFromFile(file: File): JSONObject? {
        var inputString: String? = null
        var inputStream: InputStream? = null
        try {
            inputStream = file.inputStream()
            inputString = inputStream.bufferedReader().use { it.readText() }
            return JSONObject(inputString!!)
        }  catch (e: FileNotFoundException)   {
            Log.e(this.javaClass.name, "${e}")
        }
        return null
    }

}