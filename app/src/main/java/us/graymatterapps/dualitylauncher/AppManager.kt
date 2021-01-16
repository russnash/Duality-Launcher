package us.graymatterapps.dualitylauncher

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class AppManager : Service() {
    lateinit var mainAppList: AppList
    val TAG = javaClass.simpleName

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppManager Started")
        mainAppList = AppList(applicationContext)
        appList = mainAppList
        appManager = this
        if(dualityLauncherApplication.isAppManagerListenerInitialized()) {
            appManagerListener.onStarted()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppManager Destroyed")
    }

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    interface AppManagerInterface {
        fun onStarted()
    }
}