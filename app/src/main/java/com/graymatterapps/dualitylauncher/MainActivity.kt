package com.graymatterapps.dualitylauncher

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor
import com.graymatterapps.graymatterutils.GrayMatterUtils.getVersionCode
import com.graymatterapps.graymatterutils.GrayMatterUtils.shortToast
import com.graymatterapps.graymatterutils.GrayMatterUtils.showOkDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.home_screen_menu.*

const val PREFS_FILENAME = "com.graymatterapps.dualitylauncher.prefs"
lateinit var settingsPreferences: SharedPreferences
lateinit var prefs: SharedPreferences
lateinit var mainContext: Context
lateinit var appContext: Context
const val REQUEST_PERMISSION = 1
const val CONFIGURE_WIDGET = 2

class MainActivity : AppCompatActivity(), AppDrawerAdapter.DrawerAdapterInterface,
    SharedPreferences.OnSharedPreferenceChangeListener, Animation.AnimationListener,
    GestureLayout.GestureEvents, Dock.DockInterface,
    HomePagerAdapter.HomeIconsInterface, WidgetFragment.WidgetInterface,
    SettingsDeveloper.DeveloperInterface, WidgetContainer.WidgetInterface {

    lateinit var homeFragment: HomeFragment
    lateinit var drawerFragment: DrawerFragment
    lateinit var widgetFragment: WidgetFragment
    lateinit var settingsFragment: SettingsFragment
    lateinit var displayManager: DisplayManager
    var appWidgetId: Int = 0
    lateinit var appWidgetProviderInfo: AppWidgetProviderInfo

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainContext = this
        appContext = applicationContext
        prefs = mainContext.getSharedPreferences(PREFS_FILENAME, 0)
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        initCompanion()
        appList.waitForReady()

        if (isMainDisplay()) {
            postUpdateCheck()
        }

        homeFragment = HomeFragment()
        drawerFragment = DrawerFragment()
        widgetFragment = WidgetFragment()
        settingsFragment = SettingsFragment()

        setContentView(R.layout.activity_main)
        setStatusBars()
        setNavBarBackground()
        gestureLayout.setListener(this)
        gestureLayout.setGesturesOn(true)
        setupHomeMenu()
        showHomeMenu(false)

        displayManager.registerDisplayListener(object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(p0: Int) {
                updateDisplayInfo()
            }

            override fun onDisplayRemoved(p0: Int) {
                updateDisplayInfo()
            }

            override fun onDisplayChanged(p0: Int) {
                updateDisplayInfo()
            }

        }, null)
        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
                super.onFragmentAttached(fm, f, context)
                updateFragmentList()
            }

            override fun onFragmentCreated(
                fm: FragmentManager,
                f: Fragment,
                savedInstanceState: Bundle?
            ) {
                super.onFragmentCreated(fm, f, savedInstanceState)
                updateFragmentList()
            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                super.onFragmentDestroyed(fm, f)
                updateFragmentList()
            }

            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                super.onFragmentDetached(fm, f)
                updateFragmentList()
            }

            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)
                updateFragmentList()
            }

            override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
                super.onFragmentStopped(fm, f)
                updateFragmentList()
            }
        }, true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentFrame, homeFragment)
                .commitNow()
        } else {
            showHomeFragment()
        }
        updateFragmentList()
        updateDisplayInfo()

        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)

        appList = AppList()

        if (isMainDisplay()) {
            appList.updateApps()
        } else {
            setWindowBackground()
        }
    }

    fun setupHomeMenu(){
        buttonActionSettings.setOnClickListener {
            openSettings()
            showHomeMenu(false)
        }
        buttonActionWidget.setOnClickListener {
            showWidgetFragment()
            showHomeMenu(false)
        }
        buttonActionWallpaper.setOnClickListener {
            openSettings("wallpaper")
            showHomeMenu(false)
        }
        homeMenuBackground.setOnClickListener {
            showHomeMenu(false)
        }
    }

    fun showHomeMenu(state: Boolean) {
        if(state){
            homeMenu.visibility = View.VISIBLE
        } else {
            homeMenu.visibility = View.INVISIBLE
        }
    }

    fun isMainDisplay(): Boolean {
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
        var basicColor = colorPrefToColor(
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
        var basicColor = colorPrefToColor(settingsPreferences.getString("nav_background", "Black"))
        var alpha = settingsPreferences.getInt("nav_background_alpha", 80)
        var color = ColorUtils.setAlphaComponent(basicColor, alpha)
        window.navigationBarColor = color
    }

    companion object {
        lateinit var appList: AppList
        var dragAndDropData = DragAndDropData()
        lateinit var dualWallpaper: DualWallpaper

        fun initCompanion() {
            appList = AppList()
            dualWallpaper = DualWallpaper(mainContext)
        }
    }

    fun showHomeFragment() {
        gestureLayout.setGesturesOn(true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentFrame, homeFragment, "home")
            .commitNowAllowingStateLoss()
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
            showHomeFragment()
        }

        if(widgetFragment.isVisible) {
            showHomeFragment()
        }
    }

    override fun onResume() {
        onBackPressed()
        super.onResume()
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        setStatusBars()
        setNavBarBackground()
        homeFragment.startDrag(view, clipData)
        if (drawerFragment.isVisible) {
            showHomeFragment()
        }
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        gestureLayout.setGesturesOn(true)
        setStatusBars()
        setNavBarBackground()
        if (drawerFragment.isVisible) {
            showHomeFragment()
        }
        appList.launchPackage(launchInfo, displayId)
    }

    fun showWidgetFragment() {
        gestureLayout.setGesturesOn(false)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentFrame, widgetFragment, "widget")
            .commitNowAllowingStateLoss()
    }

    override fun onIconChanged() {
        homeFragment.persistGrid(homeFragment.getCurrentHomePagerItem())
    }

    override fun onLongClick(view: View) {
        Log.d(TAG, "onLongClick registered at display: ID:${windowManager.defaultDisplay.displayId}, Name:${windowManager.defaultDisplay.name}")
        showHomeMenu(true)
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

        if (key == "show_active_fragments") {
            updateFragmentList()
        }

        if (key == "show_display_info") {
            updateDisplayInfo()
        }
    }

    override fun onAnimationRepeat(p0: Animation?) {
        // Do nothing
    }

    override fun onAnimationEnd(p0: Animation?) {
        showHomeFragment()
    }

    override fun onAnimationStart(p0: Animation?) {
        setStatusBars()
        setNavBarBackground()
    }

    fun showDrawerFragment() {
        gestureLayout.setGesturesOn(false)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentFrame, drawerFragment, "drawer")
            .commitNowAllowingStateLoss()
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.slide_up)
        fragmentFrame.startAnimation(animation)
        var basicColor = colorPrefToColor(
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

    fun openSettings(setting: String? = null) {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        if(setting != null) {
            intent.putExtra("setting", setting)
        }
        startActivity(intent)
    }

    override fun onSwipeUp() {
        showDrawerFragment()
    }

    @SuppressLint("WrongConstant")
    override fun onSwipeDown() {
        val statusBarService = getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expand = statusBarManager.getMethod("expandNotificationsPanel")
        expand.invoke(statusBarService)
    }

    override fun onAddWidget(clipData: ClipData, view: View) {
        val dsb = WidgetDragShadowBuilder(view)
        showHomeFragment()
        if(homeFragment.homePager.startDragAndDrop(clipData, dsb, false, 0)){
            Log.d(TAG, "onAddWidget() startDragAndDrop successful")
        } else {
            Log.d(TAG, "onAddWidget() startDragAndDrop failed")
        }
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
        editor.putInt("previousVersion", getVersionCode(this))
        editor.apply()
    }

    fun updateDisplayInfo() {
        if (settingsPreferences.getBoolean("show_display_info", false)) {
            displayList.visibility = View.VISIBLE
            val dispList = displayManager.displays
            val dispTags = ArrayList<String>()
            dispTags.add("This display = ID:${windowManager.defaultDisplay.displayId}, Name:${windowManager.defaultDisplay.name}")
            dispList.forEach {
                dispTags.add("ID:${it.displayId}, State:${it.state}, Name:${it.name}")
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dispTags)
            displayList.adapter = adapter
            adapter.notifyDataSetChanged()
        } else {
            displayList.visibility = View.INVISIBLE
        }
    }

    fun updateFragmentList() {
        if (settingsPreferences.getBoolean("show_active_fragments", false)) {
            fragmentList.visibility = View.VISIBLE
            val fragList = supportFragmentManager.fragments
            val fragTags = ArrayList<String>()
            fragList.forEach {
                fragTags.add("${it.id}:${it.tag}")
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fragTags)
            fragmentList.adapter = adapter
            adapter.notifyDataSetChanged()
        } else {
            fragmentList.visibility = View.INVISIBLE
        }
    }

    override fun updateAppList() {
        appList.updateApps()
        shortToast(this, "AppList update forced...")
    }

    override fun needPermissionToBind(widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo) {
        appWidgetId = widgetId
        appWidgetProviderInfo = widgetProviderInfo
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetProviderInfo.provider)
        }
        startActivityForResult(intent, REQUEST_PERMISSION)
    }

    override fun onWidgetChanged() {
        homeFragment.persistGrid(homeFragment.getCurrentHomePagerItem())
    }

    override fun configureWidget(widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo) {
        intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        intent.setComponent(widgetProviderInfo.configure)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        try {
            startActivityForResult(intent, CONFIGURE_WIDGET)
        } catch(e: Exception) {
            createWidget(null)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_PERMISSION) {
                configureWidgetData(data)
            }

            if(requestCode == CONFIGURE_WIDGET){
                createWidget(data)
            }
        } else {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun configureWidgetData(data: Intent? = null){
        val view = homeFragment.homePager.findViewWithTag<View>(homeFragment.getCurrentHomePagerItem())
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for(n in 0 until homeIconsTable.childCount){
            if(homeIconsTable.getChildAt(n) is WidgetContainer){
                val child = homeIconsTable.getChildAt(n) as WidgetContainer
                if(child.isWaitingForPermission){
                    child.configureWidget(data)
                }
            }
        }
    }

    fun createWidget(data: Intent?) {
        val view = homeFragment.homePager.findViewWithTag<View>(homeFragment.getCurrentHomePagerItem())
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for(n in 0 until homeIconsTable.childCount){
            if(homeIconsTable.getChildAt(n) is WidgetContainer){
                val child = homeIconsTable.getChildAt(n) as WidgetContainer
                if(child.isWaitingForConfigure){
                    child.createWidget(data)
                }
            }
        }
    }
}
