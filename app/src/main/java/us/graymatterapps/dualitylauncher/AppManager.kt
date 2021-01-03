package us.graymatterapps.dualitylauncher

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class AppManager : Service() {
    lateinit var mainAppList: AppList
    val TAG = javaClass.simpleName

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppManager Started")
        mainAppList = AppList(applicationContext)
        appManager = this
        appList = mainAppList
        appManagerListener.onStarted()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppManager Destroyed")
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    interface AppManagerInterface {
        fun onStarted()
    }
}