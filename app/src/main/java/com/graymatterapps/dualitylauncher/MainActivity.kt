package com.graymatterapps.dualitylauncher

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.customview.widget.ExploreByTouchHelper
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

const val TAG = "DUALITYLAUNCHER"
const val PREFS_FILENAME = "com.graymatterapps.dualitylauncher.prefs"
lateinit var settingsPreferences: SharedPreferences
lateinit var prefs: SharedPreferences
lateinit var mainContext: Context
lateinit var appWidgetHost: AppWidgetHost
lateinit var appWidgetManager: AppWidgetManager
const val REQUEST_PERMISSION = 1
const val CONFIGURE_WIDGET = 2

class MainActivity : AppCompatActivity(), AppDrawerAdapter.DrawerAdapterInterface,
    SharedPreferences.OnSharedPreferenceChangeListener, Animation.AnimationListener,
    HomeFragment.HomeInterface, GestureLayout.GestureEvents, Dock.DockInterface,
    HomePagerAdapter.HomeIconsInterface, WidgetActivity.WidgetInterface {

    lateinit var broadcastReceiver: BroadcastReceiver
    var homeFragment = HomeFragment()
    var drawerFragment = DrawerFragment()
    var settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initCompanion(this)

        prefs = context.getSharedPreferences(PREFS_FILENAME, 0)

        if (isMainDisplay()) {
            postUpdateCheck()
        }

        mainContext = this

        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)

        setContentView(R.layout.activity_main)
        setStatusBars()
        setNavBarBackground()
        homeFragment.setListener(this)
        gestureLayout.setListener(this)
        gestureLayout.setGesturesOn(true)
        appWidgetHost = AppWidgetHost(applicationContext, ExploreByTouchHelper.HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetHost.startListening()

        if(savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentFrame, homeFragment, "home")
                .commit()
        }

        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)

        appList = AppList()

        if (isMainDisplay()) {
            appList.updateApps()
        } else {
            setWindowBackground()
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isMainDisplay()) {
                    appList.updateApps()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        intentFilter.addDataScheme("package")
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        appWidgetHost.stopListening()
        super.onDestroy()
    }

    fun isMainDisplay(): Boolean {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        val currentDisplay = windowManager.getDefaultDisplay().displayId
        return displays[0].displayId == currentDisplay
    }

    fun setWindowBackground() {
        if (!isMainDisplay()) {
            if (settingsPreferences.getBoolean("dual_wallpaper_hack", true)) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                window.decorView.background = dualWallpaper.get()
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                window.decorView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    fun setStatusBars() {
        window.statusBarColor = Color.TRANSPARENT
        if (settingsPreferences.getBoolean("status_dual_screen", true)) {
            setStatusBarBackground()
        } else {
            if (isMainDisplay()) {
                setStatusBarBackground()
            }
        }
    }

    fun setStatusBarBackground() {
        var basicColor = MainActivity.colorPrefToColor(
            settingsPreferences.getString(
                "status_background",
                "Black"
            )
        )
        var alpha = settingsPreferences.getInt("status_background_alpha", 80)
        var color = ColorUtils.setAlphaComponent(basicColor, alpha)
        window.statusBarColor = color
        if (settingsPreferences.getBoolean("status_light", true)) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    fun setNavBarBackground() {
        var basicColor =
            MainActivity.colorPrefToColor(settingsPreferences.getString("nav_background", "Black"))
        var alpha = settingsPreferences.getInt("nav_background_alpha", 80)
        var color = ColorUtils.setAlphaComponent(basicColor, alpha)
        window.navigationBarColor = color
    }

    companion object {
        lateinit var context: Context
        lateinit var appList: AppList
        var dragAndDropData = DragAndDropData()
        lateinit var dualWallpaper: DualWallpaper

        fun initCompanion(con: Context) {
            context = con
            dualWallpaper = DualWallpaper(context)
        }

        fun colorPrefToColor(color: String?): Int {
            when (color) {
                "Black" -> return Color.BLACK
                "White" -> return Color.WHITE
                "Green" -> return Color.GREEN
                "Blue" -> return Color.BLUE
                "Cyan" -> return Color.CYAN
                "Dark Gray" -> return Color.DKGRAY
                "Gray" -> return Color.GRAY
                "Light Gray" -> return Color.LTGRAY
                "Magenta" -> return Color.MAGENTA
                "Red" -> return Color.RED
                "Yellow" -> return Color.YELLOW
                else -> return Color.TRANSPARENT
            }
        }

        fun vibrate(millis: Long) {
            val vibe = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibe.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        fun showOkCancelDialog(
            con: Context,
            message: String,
            okListener: DialogInterface.OnClickListener,
            cancelListener: DialogInterface.OnClickListener? = null
        ) {
            AlertDialog.Builder(con)
                .setMessage(message)
                .setPositiveButton("Ok", okListener)
                .setNegativeButton("Cancel", cancelListener)
                .create()
                .show()
        }

        fun showOkDialog(
            con: Context,
            message: String,
            okListener: DialogInterface.OnClickListener? = null
        ) {
            AlertDialog.Builder(con)
                .setMessage(message)
                .setPositiveButton("Ok", okListener)
                .create()
                .show()
        }

        fun longToast(message: String) {
            Toast.makeText(MainActivity.context, message, Toast.LENGTH_LONG).show()
        }

        fun shortToast(message: String) {
            Toast.makeText(MainActivity.context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun closeAppDrawer() {
        gestureLayout.setGesturesOn(true)
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.slide_down)
        animation.setAnimationListener(this)
        fragmentFrame.startAnimation(animation)
    }

    override fun onBackPressed() {
        if (drawerFragment.isVisible) {
            closeAppDrawer()
        }

        if (settingsFragment.isVisible) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentFrame, homeFragment, "home")
                .commit()
        }
    }

    override fun onResume() {
        onBackPressed()
        super.onResume()
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        if(drawerFragment.isVisible) {
            supportFragmentManager
                .beginTransaction()
                .hide(drawerFragment)
                .show(homeFragment)
                .commit()
        }

        setStatusBars()
        setNavBarBackground()
        homeFragment.startDrag(view, clipData)
        gestureLayout.setGesturesOn(true)
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        gestureLayout.setGesturesOn(true)
        setStatusBars()
        setNavBarBackground()
        if(drawerFragment.isVisible){
            supportFragmentManager
                .beginTransaction()
                .hide(drawerFragment)
                .show(homeFragment)
                .commit()
        }
        appList.launchPackage(launchInfo, displayId)
    }

    override fun onIconChanged() {
        homeFragment.persistGrid(homeFragment.homePager.currentItem)
    }

    override fun onLongClick() {
        openSettings()
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key == "status_background") {
            setStatusBars()
        }

        if (key == "status_background_alpha") {
            setStatusBars()
        }

        if (key == "status_light") {
            setStatusBars()
        }

        if (key == "status_dual_screen") {
            setStatusBars()
        }

        if (key == "nav_background") {
            setNavBarBackground()
        }

        if (key == "nav_background_alpha") {
            setNavBarBackground()
        }

        if (key == "dual_wallpaper_hack") {
            setWindowBackground()
        }

        if (key == "update_wallpaper") {
            setWindowBackground()
        }
    }

    override fun onAnimationRepeat(p0: Animation?) {
        // Do nothing
    }

    override fun onAnimationEnd(p0: Animation?) {
        supportFragmentManager
            .beginTransaction()
            .hide(drawerFragment)
            .show(homeFragment)
            .commit()
    }

    override fun onAnimationStart(p0: Animation?) {
        setStatusBars()
        setNavBarBackground()
    }

    fun openAppDrawer() {
        gestureLayout.setGesturesOn(false)
        if(drawerFragment.isHidden){
            supportFragmentManager
                .beginTransaction()
                .hide(homeFragment)
                .show(drawerFragment)
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .hide(homeFragment)
                .add(R.id.fragmentFrame, drawerFragment, "drawer")
                .commit()
        }
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.slide_up)
        fragmentFrame.startAnimation(animation)
        var basicColor = MainActivity.colorPrefToColor(
            settingsPreferences.getString(
                "app_drawer_background",
                "Black"
            )
        )
        var alpha = settingsPreferences.getInt("app_drawer_background_alpha", 80)
        var color = ColorUtils.setAlphaComponent(basicColor, alpha)

        if (settingsPreferences.getBoolean("app_drawer_nav_status_sync", true)) {
            window.statusBarColor = color
            window.navigationBarColor = color
        }
    }

    fun openSettings() {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onAppDrawerOpen() {
        openAppDrawer()
    }

    override fun onSwipeUp() {
        openAppDrawer()
    }

    @SuppressLint("WrongConstant")
    override fun onSwipeDown() {
        val statusBarService = getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expand = statusBarManager.getMethod("expandNotificationsPanel")
        expand.invoke(statusBarService)
    }

    override fun onAddWidget(widgetView: Int, appWidgetProviderInfo: AppWidgetProviderInfo) {
        homeFragment.addWidget(widgetView, appWidgetProviderInfo, homeFragment.homePager.currentItem)
    }

    fun postUpdateCheck() {
        val previousVersion = prefs.getInt("previousVersion", 0)
        val editor = prefs.edit()

        if (previousVersion < 7) {
            if (prefs.getString("homeIconsGrid0", "") == "") {
                // This is a fresh install, no need to clear any icon persistence
            } else {
                // Need to clear dock & home icon persistence
                showOkDialog(
                    this,
                    "Your dock and icon grid setup had to be cleared, sorry but it was the only way!"
                )

                try {
                    editor.remove("dockItems")
                    editor.remove("homeIconsGrid0")
                    editor.remove("homeIconsGrid1")
                    editor.remove("homeIconsGrid2")
                    editor.remove("homeIconsGrid3")
                    editor.remove("homeIconsGrid4")
                } catch (e: Exception) {
                    showOkDialog(
                        this,
                        "An error was encountered clearing your dock and icon grid setup, should you experience any issues please uninstall Duality Launcher and re-install.  Sorry for the inconvenience!"
                    )
                }
            }
        }
        editor.putInt("previousVersion", BuildConfig.VERSION_CODE)
        editor.apply()
    }
}
