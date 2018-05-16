package com.musicplayer.aow.ui.main.library.home.browse

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.WindowManager
import android.widget.Toast
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.musicplayer.aow.R
import com.musicplayer.aow.bus.RxBus
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.model.Song
import com.musicplayer.aow.delegates.event.PlaySongEvent
import com.musicplayer.aow.delegates.player.Player
import com.musicplayer.aow.ui.base.BaseActivity
import com.musicplayer.aow.ui.widget.DividerItemDecoration
import com.rxandroidnetworking.RxAndroidNetworking
import kotlinx.android.synthetic.main.browse_list_activity.*
import org.json.JSONObject
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


/**
 * Created by Arca on 2/16/2018.
 */
class BrowseActivity: BaseActivity(){

    var mPlayer = Player.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var window = this.window
        window.statusBarColor = Color.RED

        setContentView(R.layout.browse_list_activity)
        ButterKnife.bind(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var window = getWindow()
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.black)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black)
        toolbar.setNavigationOnClickListener {
            // back button pressed
            finish()
        }


        val intent = intent
        if (intent != null) {
            // To get the data use
            val data = intent.getStringExtra("com.musicplayer.aow.section")
            if (data != null) {
                val jsonObj = JSONObject(data)
                item_name.text = jsonObj.getString("name")
                item_owner.text = jsonObj.getString("artist")

                var song = Song(jsonObj.getString("name"),jsonObj.getString("name"),
                        jsonObj.getString("artist"),"mucicxplay discovery", jsonObj.getString("link"),
                        30000, 1000, false, 0, "", jsonObj.getString("url"))



                play_all.setOnClickListener {
                    //mPlayer!!.mPlayList = PlayList(song)
                    //Player.instance!!.playStream(PlayList(song), 0)
                    RxBus.instance!!.post(PlaySongEvent(song))
                }



                download.setOnClickListener {
                    RxAndroidNetworking.download(song.path, Environment.DIRECTORY_MUSIC
                            , song.displayName + ".mp3")
                            .build()
                            .setDownloadProgressListener { bytesDownloaded, totalBytes ->
                                // do anything with progress
                            }
                            .downloadObservable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object: Observer<String> {
                                override fun onError(e: Throwable?) {

                                }

                                override fun onNext(t: String?) {

                                }

                                override fun onCompleted() {
                                    Toast.makeText(applicationContext, "Downloaded", Toast.LENGTH_SHORT).show()
                                }

                            })
                }




                Glide.with(this)
                        .load(jsonObj.getString("url"))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .fitCenter()
                        .error(R.drawable.nigerian_artists)
                        .into(album_art)
                var songs = PlayList(song)
                for (i in 1..10)  {
                    songs.songs.add(song)
                }
                recycler_views.layoutManager = LinearLayoutManager(applicationContext)
                recycler_views.addItemDecoration(DividerItemDecoration(this.getDrawable(R.drawable.drawble_divider),false, false))
                recycler_views.adapter = BrowseAdapter(applicationContext, songs, this )
            }
        }

    }


}