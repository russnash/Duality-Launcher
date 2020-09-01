package com.graymatterapps.dualitylauncher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.UserHandle
import android.os.UserManager
import com.graymatterapps.graymatterutils.GrayMatterUtils.longToast
import java.util.concurrent.locks.ReentrantLock

class AppList() : LauncherApps.Callback() {
    val apps: ArrayList<AppListDataType> = ArrayList()
    var ready: Boolean = false
    val lock = ReentrantLock()
    var launcher: LauncherApps =
        mainContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = mainContext.getSystemService(Context.USER_SERVICE) as UserManager
    val TAG = javaClass.simpleName

    init {
        updateApps()
        launcher.registerCallback(this)
    }

    fun waitForReady(){
        while (!ready) {
            Thread.sleep(100)
        }
    }

    fun updateApps() {
        Thread(Runnable {
            lock.lock()
            apps.clear()

            val handles = launcher.profiles

            handles.forEach { handle ->
                val appList = launcher.getActivityList(null, handle)
                appList.forEach { apps ->
                    val name = apps.label.toString()
                    val icon = apps.getBadgedIcon(0)
                    val packageName = apps.applicationInfo.packageName
                    val activityName = apps.componentName.className
                    val handle = apps.user
                    val userSerial = userManager.getSerialNumberForUser(handle)
                    this.apps.add(
                        AppListDataType(
                            name,
                            icon,
                            activityName,
                            packageName,
                            handle,
                            userSerial
                        )
                    )
                }
            }

            try {
                apps.sortBy { it.name }
            } catch (e: Exception) {
                // Do nothing
            }
            val editor = prefs.edit()
            editor.putString("apps", System.currentTimeMillis().toString())
            editor.apply()
            lock.unlock()
            ready = true
        }).start()
    }

    fun getIconFromApps(launchInfo: LaunchInfo): Drawable {
        lock.lock()
        val app = apps.find {
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

    fun getLabelFromApps(launchInfo: LaunchInfo): String {
        lock.lock()
        val app = apps.find {
            it.activityName == launchInfo.getActivityName() && it.packageName == launchInfo.getPackageName() && it.userSerial == launchInfo.getUserSerial()
        }
        lock.unlock()
        if (app != null) {
            return app.name
        } else {
            return "?Unknown?"
        }
    }

    fun launchPackage(launchInfo: LaunchInfo, display: Int) {
        try {
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = display
            val componentName =
                ComponentName(launchInfo.getPackageName(), launchInfo.getActivityName())
            val handle = userManager.getUserForSerialNumber(launchInfo.getUserSerial())
            launcher.startMainActivity(componentName, handle, null, options.toBundle())
        } catch (e: Exception) {
            longToast(mainContext, "App failed to launch (" + e.message + ")")
        }
    }

    fun launchAppInfo(launchInfo: LaunchInfo, display: Int) {
        try {
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = display
            val componentName =
                ComponentName(launchInfo.getPackageName(), launchInfo.getActivityName())
            val handle = userManager.getUserForSerialNumber(launchInfo.getUserSerial())
            launcher.startAppDetailsActivity(componentName, handle, null, options.toBundle())
        } catch (e: Exception) {
            longToast(mainContext, "App Info failed to launch")
        }
    }

    data class AppListDataType(
        var name: String,
        var icon: Drawable,
        var activityName: String,
        var packageName: String,
        var handle: UserHandle,
        var userSerial: Long
    )

    override fun onPackageRemoved(p0: String?, p1: UserHandle?) {
        updateApps()
    }

    override fun onPackageAdded(p0: String?, p1: UserHandle?) {
        updateApps()
    }

    override fun onPackageChanged(p0: String?, p1: UserHandle?) {
        updateApps()
    }

    override fun onPackagesAvailable(p0: Array<out String>?, p1: UserHandle?, p2: Boolean) {
        updateApps()
    }

    override fun onPackagesUnavailable(p0: Array<out String>?, p1: UserHandle?, p2: Boolean) {
        updateApps()
    }
}