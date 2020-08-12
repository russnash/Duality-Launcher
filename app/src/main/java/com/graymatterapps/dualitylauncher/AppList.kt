package com.graymatterapps.dualitylauncher

import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.UserHandle
import android.os.UserManager
import java.util.concurrent.locks.ReentrantLock

class AppList (val pm: PackageManager, val prefs: SharedPreferences, val mainActivity: Activity){
    val apps: ArrayList<AppListDataType> = ArrayList()
    var ready: Boolean = false
    val lock = ReentrantLock()

    init{
        updateApps()
    }

    fun updateApps(){
        Thread(Runnable {
            lock.lock()
            apps.clear()

            val launcher: LauncherApps = mainContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val handles = launcher.profiles
            val userMananger = mainContext.getSystemService(Context.USER_SERVICE) as UserManager

            handles.forEach { handle ->
                val appList = launcher.getActivityList(null, handle)
                appList.forEach { apps ->
                    val name = apps.label.toString()
                    val icon = apps.getBadgedIcon(0)
                    val packageName = apps.applicationInfo.packageName
                    val activityName = apps.componentName.className
                    val handle = apps.user
                    val userSerial = userMananger.getSerialNumberForUser(handle)
                    this.apps.add(AppListDataType(name, icon, activityName, packageName, handle, userSerial))
                }
            }

            try{
                apps.sortBy { it.name }
            } catch (e: Exception) {
                // Do nothing
            }
            mainActivity.runOnUiThread(java.lang.Runnable{
                val editor = prefs.edit()
                editor.putString("apps", System.currentTimeMillis().toString())
                editor.apply()
            })
            lock.unlock()
            ready = true
        }).start()
    }

    fun getIconFromApps(launchInfo: LaunchInfo) : Drawable {
        lock.lock()
        val app = apps.find{
            it.activityName == launchInfo.getActivityName() && it.packageName == launchInfo.getPackageName() && it.userSerial == launchInfo.getUserSerial()
        }
        lock.unlock()
        if (app != null) {
            return app.icon
        } else {
            val shapeDrawable = ShapeDrawable(OvalShape())
            shapeDrawable.paint.color = Color.TRANSPARENT
            return shapeDrawable
        }
    }

    fun getLabelFromApps(launchInfo: LaunchInfo) : String {
        lock.lock()
        val app = apps.find{
            it.activityName == launchInfo.getActivityName() && it.packageName == launchInfo.getPackageName() && it.userSerial == launchInfo.getUserSerial()
        }
        lock.unlock()
        if(app != null) {
            return app.name
        } else {
            return "?Unknown?"
        }
    }

    fun launchPackage(launchInfo: LaunchInfo, display: Int) {
        try {
            val launcher: LauncherApps =
                mainContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val userManager: UserManager = mainContext.getSystemService(Context.USER_SERVICE) as UserManager
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = display
            val componentName =
                ComponentName(launchInfo.getPackageName(), launchInfo.getActivityName())
            val handle = userManager.getUserForSerialNumber(launchInfo.getUserSerial())
            launcher.startMainActivity(componentName, handle, null, options.toBundle())
        } catch (e: Exception) {
            MainActivity.longToast("App failed to launch (" + e.message + ")")
        }
    }

    fun launchAppInfo(packageName: String, display: Int) {
        try{
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = display
            val intent: Intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            MainActivity.context.startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            MainActivity.longToast("App Info failed to launch")
        }
    }

    data class AppListDataType(var name: String, var icon: Drawable, var activityName: String, var packageName: String, var handle: UserHandle, var userSerial: Long)
}