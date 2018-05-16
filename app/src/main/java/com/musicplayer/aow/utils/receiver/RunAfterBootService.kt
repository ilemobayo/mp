package com.musicplayer.aow.utils.receiver

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.musicplayer.aow.application.Injection
import com.musicplayer.aow.delegates.data.model.PlayList
import com.musicplayer.aow.delegates.data.model.TempSongs
import com.musicplayer.aow.delegates.data.source.AppRepository
import com.musicplayer.aow.utils.StorageUtil
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*

class RunAfterBootService : Service {

    var counter = 0
    //var context: Context = applicationContext

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private val mSubscriptions: CompositeSubscription? = null

    constructor() : super() {
        //context = applicationContext
        Log.d("HERE", "here service created!")
    }

    override fun onCreate() {
        super.onCreate()
        //runCheck()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //startTimer()
        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("EXIT", "ondestroy!")

        val broadcastIntent = Intent("musix.play.Activit.Restart")
        sendBroadcast(broadcastIntent)
        stoptimertask()
    }

    fun startTimer() {
        //set a new Timer
        timer = Timer()

        //initialize the TimerTask's job
        initializeTimerTask()

        //schedule the timer, to wake up every 1 second
        timer!!.schedule(timerTask, 50000) //
    }

    fun initializeTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {

                //runCheck()

            }
        }
    }

    fun runCheck(){
        var storage = StorageUtil(Injection.provideContext()!!)
        if(storage.loadStringValue("init").equals("empty",true)) {
            AppRepository().createDefaultAllSongs()
            doAsync {
                TempSongs.instance!!.setSongs()
                onComplete {
                    updateAllSongsPlayList(PlayList(TempSongs.instance!!.songs))
                    storage.saveStringValue("init","not empty")
                }
            }
            Log.e("X-service", "init")
        }else{
            doAsync {
                TempSongs.instance!!.setSongs()
                onComplete {
                    updateAllSongsPlayList(PlayList(TempSongs.instance!!.songs))
                    storage.saveStringValue("init","not empty")
                }
            }
            Log.e("X-service", "non-init")
        }
    }

    private fun updateAllSongsPlayList(playList: PlayList) {
        val subscription = AppRepository().setInitAllSongs(playList).subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(object : Subscriber<PlayList>() {
                    override fun onStart() {}

                    override fun onCompleted() { }

                    override fun onError(e: Throwable) {}

                    override fun onNext(result: PlayList) {
                    }
                })
        mSubscriptions?.add(subscription)
    }

    fun stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}