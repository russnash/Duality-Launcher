package us.graymatterapps.dualitylauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StartOnBoot: BroadcastReceiver() {
    val TAG = javaClass.simpleName

    override fun onReceive(con: Context?, intent: Intent?) {
        if (intent != null) {
            Log.d(TAG, "onReceive() ${intent.action}")
            if(intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                dualityLauncherApplication.startAppManager()
            }
        }
    }
}