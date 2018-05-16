package com.musicplayer.aow.ui.splashscreen

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.musicplayer.aow.application.Injection
import com.musicplayer.aow.ui.main.MainActivity
import com.musicplayer.aow.utils.receiver.RunAfterBootService


/**
 * Created by Arca on 10/2/2017.
 */

class SplashScreen : Activity() {

    val mSensorService = RunAfterBootService()
    val mServiceIntent = Intent(Injection.provideContext(), mSensorService.javaClass)

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private val REQUEST_STORAGE_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        ActivityCompat.requestPermissions(this, permissions, REQUEST_STORAGE_PERMISSION)
        
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        //permission
        startNextActivity()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        if (!permissionToRecordAccepted) finish()

    }

    private fun permission(){
        var intent = Intent(applicationContext,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityCompat.startActivity(applicationContext,intent,null)
    }

    private fun startNextActivity()
    {
        Handler().postDelayed(
        {
            permission()
        }, timeoutMillis)
    }

    private fun isBServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.e("isServiceRunning?", true.toString() + "")
                return true
            }
        }
        Log.e("isServiceRunning?", false.toString() + "")
        return false
    }

    companion object {
        // Splash screen timer
        private var timeoutMillis = 1000L

        var instance: SplashScreen? = null
            private set
    }
}
