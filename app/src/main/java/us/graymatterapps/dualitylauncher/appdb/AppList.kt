package us.graymatterapps.dualitylauncher.appdb

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
import us.graymatterapps.dualitylauncher.*
import us.graymatterapps.graymatterutils.GrayMatterUtils.longToast
import us.graymatterapps.graymatterutils.GrayMatterUtils.shortToast
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class AppList(val context: Context) : LauncherApps.Callback() {
    var manualWorkApps: ArrayList<LaunchInfo> = ArrayList()
    var appDB = AppDB(context)
    private var ready: Boolean = false
    val lock = ReentrantLock()
    private var launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val packageManager: PackageManager = context.packageManager
    val TAG: String = javaClass.simpleName

    init {
        iconPackManager.updateIconPacks()
        if(appDB.getRecordCount() == 0L) {
            createAppDB()
        } else {
            verifyAppDB()
        }
        depersistManualWorkApps()
        appDB.updateWorkApps(manualWorkApps)
        launcherApps.registerCallback(this)
        ready = true
    }

    fun waitForReady() {
        Log.d(TAG, "Waiting for ready...")
        while (!ready) {
            Thread.sleep(100)
        }
        Log.d(TAG, "...ready!")
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
        appDB.setAsManualWorkApp(info)
        persistManualWorkApps()
    }

    fun desetAsManualWorkApp(info: LaunchInfo) {
        manualWorkApps.remove(info)
        appDB.desetAsManualWorkApp(info)
        persistManualWorkApps()
    }

    fun createAppDB() {
        Log.d(TAG, "createAppDB()")

        var activeIconPack = settingsPreferences.getString("choose_icon_pack", "Default")
        if (activeIconPack != "Default") {
            val debug: Boolean = false
            if (debug) {
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

        lock.lock()

        val handles = launcherApps.profiles
        handles.forEach { handle ->
            val appList = launcherApps.getActivityList(null, handle)
            appList.forEach { app ->
                Log.d("AppDump", "App: ${app.name} ${app.applicationInfo.packageName} ${app.user.toString()}")
                addAppDBEntry(app, activeIconPack!!)
            }
        }

        val drawerIcon = AppListDataType(
            "All Apps",
            ContextCompat.getDrawable(appContext, R.mipmap.ic_app_drawer_button)!!,
            "allapps",
            "allapps",
            Process.myUserHandle(),
            0L
        )
        appDB.add(drawerIcon)

        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
        lock.unlock()
    }

    private fun addAppDBEntry(app: LauncherActivityInfo, activeIconPack: String) {
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
                if (icon.foreground != null) {
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
        if(packageName == "com.android.contacts") {
            Log.d(TAG, "appDB.add $name, $userSerial, ${handle.toString()}")
        }
        appDB.add(
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

    private fun updateAppDBEntry(app: LauncherActivityInfo, activeIconPack: String) {
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
                if (icon.foreground != null) {
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
        appDB.update(
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
        return appDB.isAppInDB(launchInfo.getPackageName(), launchInfo.getActivityName())
    }

    fun getIcon(launchInfo: LaunchInfo): Drawable {
        return appDB.getIcon(launchInfo.getActivityName(),launchInfo.getPackageName(), launchInfo.getUserSerial())
    }

    fun getName(launchInfo: LaunchInfo): String {
        return appDB.getName(launchInfo.getActivityName(),launchInfo.getPackageName(), launchInfo.getUserSerial())
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

    fun verifyAppDB() {
        Log.d(TAG, "verifyAppDB() started")
        var activeIconPack = settingsPreferences.getString("choose_icon_pack", "Default")
        if (activeIconPack != "Default") {
            val debug: Boolean = false
            if (debug) {
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

        thread{
            var appList: ArrayList<LauncherActivityInfo> = ArrayList()
            val handles = launcherApps.profiles

            handles.forEach { handle ->
                val handleAppList = launcherApps.getActivityList(null, handle)
                appList.addAll(handleAppList)
            }

            appList.forEach{
                updateAppDBEntry(it, activeIconPack!!)
            }

            val cursor = appDB.getAllPackages()

            try {
                while (cursor.moveToNext()) {
                    val packageName = cursor.getString(0)
                    val userSerial = cursor.getInt(1)
                    var found = false
                    appList.forEach {
                        if (it.componentName.packageName == packageName && userManager.getSerialNumberForUser(it.user) == userSerial.toLong()) {
                            found = true
                        }
                    }
                    if (!found) {
                        appDB.removePackage(packageName, userSerial.toLong())
                    }
                }
            } finally {
                cursor.close()
            }

            val drawerIcon = AppListDataType(
                "All Apps",
                ContextCompat.getDrawable(appContext, R.mipmap.ic_app_drawer_button)!!,
                "allapps",
                "allapps",
                Process.myUserHandle(),
                0L
            )
            appDB.add(drawerIcon)

            appDB.updateWorkApps(manualWorkApps)

            val editor = prefs.edit()
            editor.putString("apps", System.currentTimeMillis().toString())
            editor.apply()

            Log.d(TAG, "verifyAppDB() completed")
        }
    }

    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
        removePackage(packageName!!, user!!)
        iconPackManager.updateIconPacks()
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    private fun removePackage(packageName: String, user: UserHandle) {
        Log.d(TAG, "removePackage $packageName")
        appDB.removePackage(packageName, userManager.getSerialNumberForUser(user))

        val removed = ArrayList<IconPackManager.IconPack>()
        iconPackManager.iconPacks.forEach {
            if (it.packageName == packageName) {
                removed.add(it)
                if (settingsPreferences.getString("choose_icon_pack", "Default") == it.name) {
                    val editor = settingsPreferences.edit()
                    editor.putString("choose_icon_pack", "Default")
                    editor.apply()
                    createAppDB()
                }
            }
        }
        iconPackManager.iconPacks.removeAll(removed)
    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
        addPackage(packageName!!, user!!)
        iconPackManager.updateIconPacks()
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
            addAppDBEntry(it, activeIconPack!!)
        }
        this.lock.unlock()
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {
        removePackage(packageName!!, user!!)
        addPackage(packageName, user)
        iconPackManager.updateIconPacks()
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    override fun onPackagesAvailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean
    ) {
        packageNames?.forEach {
            addPackage(it, user!!)
        }
        iconPackManager.updateIconPacks()
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }

    override fun onPackagesUnavailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean
    ) {
        packageNames?.forEach {
            removePackage(it, user!!)
        }
        iconPackManager.updateIconPacks()
        val editor = prefs.edit()
        editor.putString("apps", System.currentTimeMillis().toString())
        editor.apply()
    }
}