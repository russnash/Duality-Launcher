package us.graymatterapps.dualitylauncher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import us.graymatterapps.graymatterutils.GrayMatterUtils.longToast
import us.graymatterapps.graymatterutils.GrayMatterUtils.shortToast
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList


class AppList(val context: Context) : LauncherApps.Callback() {
    val apps: ArrayList<AppListDataType> = ArrayList()
    var manualWorkApps: ArrayList<LaunchInfo> = ArrayList()
    var ready: Boolean = false
    val lock = ReentrantLock()
    var launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val packageManager = context.packageManager
    val TAG = javaClass.simpleName

    init {
        updateApps()
        depersist()
        launcherApps.registerCallback(this)
    }

    fun waitForReady() {
        while (!ready) {
            Thread.sleep(100)
        }
    }

    fun depersist() {
        var loadItJson = prefs.getString("manualWorkApps", "")
        if (loadItJson != "") {
            manualWorkApps = loadItJson?.let { Json.decodeFromString(it) }!!
        }
    }

    fun persist() {
        var saveItJson = Json.encodeToString(manualWorkApps)
        val editor = prefs.edit()
        editor.putString("manualWorkApps", saveItJson)
        editor.apply()
    }

    fun isManualWorkApp(info: LaunchInfo): Boolean {
        return manualWorkApps.contains(info)
    }

    fun setAsManualWorkApp(info: LaunchInfo) {
        manualWorkApps.add(info)
        persist()
    }

    fun desetAsManualWorkApp(info: LaunchInfo) {
        manualWorkApps.remove(info)
        persist()
    }

    fun updateApps() {
        iconPackManager.updateIconPacks()

        var activeIconPack = settingsPreferences.getString("choose_icon_pack", "Default")
        if (activeIconPack != "Default") {
            try {
                val pkgInfo = packageManager.getPackageInfo(
                    iconPackManager.getPackageNameForIconPack(activeIconPack!!), 0
                )
                iconPackManager.initializeIconPack(activeIconPack!!)
            } catch (e: Exception) {
                activeIconPack = "Default"
                val editor = settingsPreferences.edit()
                editor.putString("choose_icon_pack", activeIconPack)
                editor.apply()
            }
        }

        if (activeIconPack == "Default") {
            iconPackManager.appFilter.clear()
            iconPackManager.scale = 0f
            iconPackManager.iconMask = null
            iconPackManager.iconBack.clear()
            iconPackManager.iconSize = 0
        }

        Thread(Runnable {
            lock.lock()
            apps.clear()

            val handles = launcherApps.profiles
            handles.forEach { handle ->
                val appList = launcherApps.getActivityList(null, handle)
                appList.forEach { apps ->
                    val packageName = apps.applicationInfo.packageName
                    val name = apps.label.toString()
                    val activityName = apps.componentName.className
                    var icon = apps.getBadgedIcon(0)

                    if (activeIconPack != "Default") {
                        try {
                            val launchIntentForPackage =
                                packageManager.getLaunchIntentForPackage(packageName)
                            val fullPathToActivity = launchIntentForPackage!!.component!!.className
                            val activityInfo = packageManager.getActivityInfo(
                                ComponentName(
                                    packageName,
                                    fullPathToActivity
                                ), 0
                            )
                            val iconRes = activityInfo.iconResource
                            icon = packageManager.getDrawable(
                                packageName,
                                iconRes,
                                activityInfo.applicationInfo
                            )
                        } catch (e: Exception) {
                            Log.d(TAG, "Couldn't get PackageManager icon for $name")
                            icon = apps.getBadgedIcon(0)
                        }

                        if (icon is AdaptiveIconDrawable) {
                            if(icon.foreground != null) {
                                icon = iconPackManager.getIconPackDrawable(
                                    activeIconPack!!,
                                    packageName,
                                    activityName,
                                    icon.foreground
                                )
                            } else {
                                Log.d(TAG, "AdaptiveIcon for $name had null foreground.")
                                icon = iconPackManager.getIconPackDrawable(
                                    activeIconPack!!,
                                    packageName,
                                    activityName,
                                    icon
                                )
                            }
                        } else {
                            icon = iconPackManager.getIconPackDrawable(
                                activeIconPack!!,
                                packageName,
                                activityName,
                                icon!!
                            )
                        }
                        if (icon == null) {
                            icon = apps.getBadgedIcon(0)
                        }
                    }

                    val handle = apps.user
                    val userSerial = userManager.getSerialNumberForUser(handle)
                    this.apps.add(
                        AppListDataType(
                            name,
                            icon!!,
                            activityName,
                            packageName,
                            handle,
                            userSerial
                        )
                    )
                }
            }

            try {
                apps.sortBy { it.name.toLowerCase() }
            } catch (e: Exception) {
                // Do nothing
            }
            val drawerIcon = AppListDataType(
                "All Apps",
                ContextCompat.getDrawable(appContext, R.mipmap.ic_app_drawer_button)!!,
                "allapps",
                "allapps",
                android.os.Process.myUserHandle(),
                0L
            )
            apps.add(0, drawerIcon)
            val editor = prefs.edit()
            editor.putString("apps", System.currentTimeMillis().toString())
            editor.apply()
            lock.unlock()
            ready = true
        }).start()
    }

    fun isAppInstalled(launchInfo: LaunchInfo): Boolean {
        apps.forEach {
            if (it.packageName == launchInfo.getPackageName() && it.activityName == launchInfo.getActivityName()) {
                return true
            }
        }
        return false
    }

    fun getIcon(launchInfo: LaunchInfo): Drawable {
        lock.lock()
        val app = apps.find {
            it.activityName == launchInfo.getActivityName() && it.packageName == launchInfo.getPackageName() && it.userSerial == launchInfo.getUserSerial()
        }
        lock.unlock()
        if (app != null) {
            return app.icon
        } else {
            return ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)!!
        }
    }

    fun getLabel(launchInfo: LaunchInfo): String {
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
            launcherApps.startMainActivity(componentName, handle, null, options.toBundle())
        } catch (e: Exception) {
            longToast(context, "App failed to launch (" + e.message + ")")
        }
    }

    fun launchAppInfo(con: Context, launchInfo: LaunchInfo, display: Int) {
        try {
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = display
            val componentName =
                ComponentName(launchInfo.getPackageName(), launchInfo.getActivityName())
            val handle = userManager.getUserForSerialNumber(launchInfo.getUserSerial())
            launcherApps.startAppDetailsActivity(componentName, handle, null, options.toBundle())
        } catch (e: Exception) {
            longToast(con, "App Info failed to launch")
        }
    }

    fun launchPackageOtherDisplay(
        parentActivity: MainActivity,
        launchInfo: LaunchInfo,
        display: Int
    ) {
        var targetDisplay: Int = 0
        var isDisplayAvailable: Boolean = true

        if (display == 1) {
            targetDisplay = 0
        } else {
            targetDisplay = 1
        }

        val displays = parentActivity.displayManager.displays
        try {
            displays.get(targetDisplay)
        } catch (e: IndexOutOfBoundsException) {
            isDisplayAvailable = false
        }

        if (isDisplayAvailable) {
            if (displays[targetDisplay].state != 2) {
                isDisplayAvailable = false
            }
        }

        if (isDisplayAvailable) {
            launchPackage(launchInfo, targetDisplay)
        } else {
            shortToast(parentActivity, "Other display is not available")
        }
    }

    fun launchDualLaunch(
        parentActivity: MainActivity,
        launchLeft: LaunchInfo,
        launchRight: LaunchInfo
    ) {
        var isDisplayAvailable: Boolean = true

        val displays = parentActivity.displayManager.displays
        try {
            displays.get(1)
        } catch (e: IndexOutOfBoundsException) {
            isDisplayAvailable = false
        }

        if (isDisplayAvailable) {
            if (displays[1].state != 2) {
                isDisplayAvailable = false
            }
        }

        if (isDisplayAvailable) {
            if (launchLeft.getActivityName() != "") {
                launchPackage(launchLeft, 1)
            }
            if (launchRight.getActivityName() != "") {
                launchPackage(launchRight, 0)
            }
        } else {
            shortToast(parentActivity, "Dual display is not available")
        }
    }

    fun getAppShortcuts(packageName: String): List<Shortcut> {
        val shortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        shortcutQuery.setPackage(packageName)
        return try {
            launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())!!
                .map { Shortcut(it.id, it.`package`, it.shortLabel.toString(), it) }
        } catch (e: SecurityException) {
            Collections.emptyList()
        }
    }

    fun loadShortcutIcon(shortcutInfo: ShortcutInfo): Drawable? {
        var drawable: Drawable? = null
        try {
            drawable = launcherApps.getShortcutIconDrawable(
                shortcutInfo,
                context.resources.displayMetrics.densityDpi
            )
        } catch (e: Exception) {
            drawable = null
        }
        return drawable
    }

    fun startShortcut(shortcut: Shortcut) {
        launcherApps.startShortcut(
            shortcut.packageName,
            shortcut.id,
            null,
            null,
            Process.myUserHandle()
        )
    }

    data class AppListDataType(
        var name: String,
        var icon: Drawable,
        var activityName: String,
        var packageName: String,
        var handle: UserHandle,
        var userSerial: Long
    )

    data class Shortcut(
        val id: String,
        val packageName: String,
        val label: String,
        val shortcutInfo: ShortcutInfo
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