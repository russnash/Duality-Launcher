package us.graymatterapps.dualitylauncher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
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
    var apps: ArrayList<AppListDataType> = ArrayList()
    private var manualWorkApps: ArrayList<LaunchInfo> = ArrayList()
    private var ready: Boolean = false
    val lock = ReentrantLock()
    private var launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val packageManager: PackageManager = context.packageManager
    val TAG: String = javaClass.simpleName

    init {
        updateApps()
        depersistManualWorkApps()
        launcherApps.registerCallback(this)
    }

    fun waitForReady() {
        while (!ready) {
            Thread.sleep(100)
        }
    }

    private fun depersistManualWorkApps() {
        val loadItJson = prefs.getString("manualWorkApps", "")
        if (loadItJson != "") {
            manualWorkApps = loadItJson?.let { Json.decodeFromString(it) }!!
        }
    }

    private fun persistManualWorkApps() {
        val saveItJson = Json.encodeToString(manualWorkApps)
        val editor = prefs.edit()
        editor.putString("manualWorkApps", saveItJson)
        editor.apply()
    }

    fun isManualWorkApp(info: LaunchInfo): Boolean {
        return manualWorkApps.contains(info)
    }

    fun setAsManualWorkApp(info: LaunchInfo) {
        manualWorkApps.add(info)
        persistManualWorkApps()
    }

    fun desetAsManualWorkApp(info: LaunchInfo) {
        manualWorkApps.remove(info)
        persistManualWorkApps()
    }

    fun updateApps() {
        Log.d(TAG, "updateApps()")
        iconPackManager.updateIconPacks()

        var activeIconPack = settingsPreferences.getString("choose_icon_pack", "Default")
        if (activeIconPack != "Default") {
            val debug: Boolean = false
            if(debug) {
                val pkgInfo = packageManager.getPackageInfo(
                    iconPackManager.getPackageNameForIconPack(activeIconPack!!), 0
                )
                iconPackManager.initializeIconPack(activeIconPack)
            } else {
                try {
                    val pkgInfo = packageManager.getPackageInfo(
                        iconPackManager.getPackageNameForIconPack(activeIconPack!!), 0
                    )
                    iconPackManager.initializeIconPack(activeIconPack)
                } catch (e: Exception) {
                    activeIconPack = "Default"
                    val editor = settingsPreferences.edit()
                    editor.putString("choose_icon_pack", activeIconPack)
                    editor.apply()
                }
            }
        }

        if (activeIconPack == "Default") {
            iconPackManager.appFilter.clear()
            iconPackManager.scale = 0f
            iconPackManager.iconMask = null
            iconPackManager.iconBack.clear()
            iconPackManager.iconSize = 0
        }

        Thread {
            lock.lock()
            apps.clear()

            val handles = launcherApps.profiles
            handles.forEach { handle ->
                val appList = launcherApps.getActivityList(null, handle)
                appList.forEach { app ->
                    updateApp(app, activeIconPack!!)
                }
            }

            try {
                apps.sortBy { it.name.toLowerCase(Locale.ROOT) }
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
        }.start()
    }

    private fun updateApp(app: LauncherActivityInfo, activeIconPack: String) {
        val packageName = app.applicationInfo.packageName
        val name = app.label.toString()
        val activityName = app.componentName.className
        var icon = app.getBadgedIcon(0)

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
                icon = app.getBadgedIcon(0)
            }

            if (icon is AdaptiveIconDrawable) {
                if(icon.foreground != null) {
                    icon = iconPackManager.getIconPackDrawable(
                        activeIconPack,
                        packageName,
                        activityName,
                        icon.foreground
                    )
                } else {
                    Log.d(TAG, "AdaptiveIcon for $name had null foreground.")
                    icon = iconPackManager.getIconPackDrawable(
                        activeIconPack,
                        packageName,
                        activityName,
                        icon
                    )
                }
            } else {
                icon = iconPackManager.getIconPackDrawable(
                    activeIconPack,
                    packageName,
                    activityName,
                    icon!!
                )
            }
            if (icon == null) {
                icon = app.getBadgedIcon(0)
            }
        }

        val handle = app.user
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
        return if (app != null) {
            app.icon
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)!!
        }
    }

    fun getLabel(launchInfo: LaunchInfo): String {
        lock.lock()
        val app = apps.find {
            it.activityName == launchInfo.getActivityName() && it.packageName == launchInfo.getPackageName() && it.userSerial == launchInfo.getUserSerial()
        }
        lock.unlock()
        return if (app != null) {
            app.name
        } else {
            "?Unknown?"
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

        targetDisplay = if (display == 1) {
            0
        } else {
            1
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
            displays[1]
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
        drawable = try {
            launcherApps.getShortcutIconDrawable(
                shortcutInfo,
                context.resources.displayMetrics.densityDpi
            )
        } catch (e: Exception) {
            null
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

    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
        removePackage(packageName!!, user!!)
        iconPackManager.updateIconPacks()
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    private fun removePackage(packageName: String, user: UserHandle) {
        Log.d(TAG, "removePackage $packageName")
        this.lock.lock()
        val removed = ArrayList<AppListDataType>()
        apps.forEach {
            if(it.packageName == packageName) {
                removed.add(it)
            }
        }
        apps.removeAll(removed)
        this.lock.unlock()

        val removed2 = ArrayList<IconPackManager.IconPack>()
        iconPackManager.iconPacks.forEach {
            if(it.packageName == packageName) {
                removed2.add(it)
                if(settingsPreferences.getString("choose_icon_pack", "Default") == it.name) {
                    val editor = settingsPreferences.edit()
                    editor.putString("choose_icon_pack", "Default")
                    editor.apply()
                    updateApps()
                }
            }
        }
        iconPackManager.iconPacks.removeAll(removed2)
    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
        addPackage(packageName!!, user!!)
        iconPackManager.updateIconPacks()
        apps.sortBy { it.name.toLowerCase(Locale.ROOT) }
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    private fun addPackage(packageName: String, user: UserHandle) {
        Log.d(TAG, "addPackage $packageName")
        this.lock.lock()
        val app = launcherApps.getActivityList(packageName, user)
        val activeIconPack = settingsPreferences.getString("choose_icon_pack", "Default")
        app.forEach {
            updateApp(it, activeIconPack!!)
        }
        this.lock.unlock()
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {
        removePackage(packageName!!, user!!)
        addPackage(packageName, user)
        iconPackManager.updateIconPacks()
        apps.sortBy { it.name.toLowerCase(Locale.ROOT) }
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
        packageNames?.forEach {
            addPackage(it, user!!)
        }
        iconPackManager.updateIconPacks()
        apps.sortBy { it.name.toLowerCase(Locale.ROOT) }
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
        packageNames?.forEach {
            removePackage(it, user!!)
        }
        iconPackManager.updateIconPacks()
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }
}