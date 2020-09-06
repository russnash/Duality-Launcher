package com.graymatterapps.dualitylauncher

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat.startDragAndDrop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor
import com.graymatterapps.graymatterutils.GrayMatterUtils.getVersionCode
import com.graymatterapps.graymatterutils.GrayMatterUtils.shortToast
import com.graymatterapps.graymatterutils.GrayMatterUtils.showOkDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_drawer.*
import kotlinx.android.synthetic.main.home_folder.*
import kotlinx.android.synthetic.main.home_screen_menu.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val PREFS_FILENAME = "com.graymatterapps.dualitylauncher.prefs"
lateinit var mainContext: Context
lateinit var homeActivity: MainActivity
lateinit var appContext: Context
const val REQUEST_PERMISSION = 1
const val CONFIGURE_WIDGET = 2
var isFolderOpen = false

class MainActivity : AppCompatActivity(), AppDrawerAdapter.DrawerAdapterInterface,
    SharedPreferences.OnSharedPreferenceChangeListener, Animation.AnimationListener,
    GestureLayout.GestureEvents, Dock.DockInterface,
    HomePagerAdapter.HomeIconsInterface, WidgetFragment.WidgetInterface,
    SettingsDeveloper.DeveloperInterface, WidgetContainer.WidgetInterface,
    FolderAdapter.FolderAdapterInterface {

    lateinit var drawerFragment: DrawerFragment
    lateinit var widgetFragment: WidgetFragment
    lateinit var settingsFragment: SettingsFragment
    lateinit var displayManager: DisplayManager
    var appWidgetId: Int = 0
    lateinit var appWidgetProviderInfo: AppWidgetProviderInfo
    lateinit var homePagerAdapter: HomePagerAdapter
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 80)

    val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainContext = this
        appContext = applicationContext
        homeActivity = this
        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        initCompanion()

        if (isMainDisplay()) {
            postUpdateCheck()
        }

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
        showHomeFolder(false)

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

        appList.waitForReady()

        dock.depersistDock()
        dock.populateDock()
        dock.setListener(this)

        homePager.offscreenPageLimit = 5
        homePagerAdapter = HomePagerAdapter(this as AppCompatActivity, frameLayout)
        homePager.adapter = homePagerAdapter
        homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                setupHomePageIndicator()
            }
        })
        homePagerAdapter.setListener(this)

        homePager.adapter?.notifyDataSetChanged()

        setupHomePageIndicator()

        homeDelete.setColorFilter(
            colorPrefToColor(
                settingsPreferences.getString(
                    "home_widget_color",
                    "White"
                )
            )
        )
        homeDelete.alpha = 0f

        homeDelete.setOnDragListener(object : View.OnDragListener {
            override fun onDrag(view: View?, dragEvent: DragEvent?): Boolean {
                if (dragEvent != null) {
                    when (dragEvent.action) {
                        DragEvent.ACTION_DRAG_STARTED -> {
                            homeDelete.alpha = 1.0f
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            homeDelete.setBackgroundColor(Color.GREEN)
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            homeDelete.setBackgroundColor(Color.TRANSPARENT)
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            homeDelete.alpha = 0f
                        }
                        DragEvent.ACTION_DROP -> {
                            homeDelete.alpha = 0f
                            homeDelete.setBackgroundColor(Color.TRANSPARENT)
                            if (dragEvent.clipDescription.label.toString().equals("widget")) {
                                val id = dragEvent.clipData.getItemAt(0).text.toString()
                                val widgetInfo = dragAndDropData.retrieveWidgetId(id)
                                appWidgetHost.deleteAppWidgetId(widgetInfo.getAppWidgetId())
                            }
                            if(dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                                val id = dragEvent.clipData.getItemAt(0).text.toString()
                                val info = dragAndDropData.retrieveLaunchInfo(id)
                                if(info.getType() == LaunchInfo.FOLDER){
                                    val editor = prefs.edit()
                                    editor.remove("folder" + info.getFolderUniqueId())
                                    editor.apply()
                                }
                            }
                        }
                    }
                }
                return true
            }
        })

        updateFragmentList()
        updateDisplayInfo()

        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)

        setWindowBackground()
    }

    fun setupHomePageIndicator() {
        homePageIndicator.removeAllViews()
        var params = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val filterColor = colorPrefToColor(
            settingsPreferences.getString(
                "home_widget_color",
                "White"
            )
        )

        for (n in 1..homePagerAdapter.itemCount) {
            var pageIndicator = ImageView(this)
            pageIndicator.layoutParams = params
            if (homePager.currentItem == n - 1) {
                pageIndicator.setImageResource(R.drawable.pager_active)
            } else {
                pageIndicator.setImageResource(R.drawable.pager_inactive)
            }
            pageIndicator.setColorFilter(filterColor)
            homePageIndicator.addView(pageIndicator)
        }
    }

    fun persistGrid(position: Int) {
        Log.d(TAG, "persistGrid()")
        var homeIconsGrid = HomeIconsGrid()
        var homeWidgetsGrid = HomeWidgetsGrid()
        val view = homePager.findViewWithTag<View>(position)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (n in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(n) is Icon) {
                val child = homeIconsTable.getChildAt(n) as Icon
                val childParams = child.layoutParams as HomeLayout.LayoutParams
                homeIconsGrid.changeLaunchInfo(
                    childParams.row,
                    childParams.column,
                    child.getLaunchInfo()
                )
            }
            if (homeIconsTable.getChildAt(n) is Folder) {
                val child = homeIconsTable.getChildAt(n) as Folder
                val childParams = child.layoutParams as HomeLayout.LayoutParams
                homeIconsGrid.changeLaunchInfo(
                    childParams.row,
                    childParams.column,
                    child.getLaunchInfo()
                )
            }
            if (homeIconsTable.getChildAt(n) is WidgetContainer) {
                val child = homeIconsTable.getChildAt(n) as WidgetContainer
                val childParams = child.layoutParams as HomeLayout.LayoutParams
                homeWidgetsGrid.changeWidgetId(
                    childParams.row,
                    childParams.column,
                    child.appWidgetId
                )
            }
        }
        var saveItJson = Json.encodeToString(homeIconsGrid)
        val editor = prefs.edit()
        editor.putString("homeIconsGrid" + position, saveItJson)
        saveItJson = Json.encodeToString(homeWidgetsGrid)
        editor.putString("homeWidgetsGrid" + position, saveItJson)
        editor.apply()
    }

    fun setupHomeMenu() {
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
        if (state) {
            homeMenu.visibility = View.VISIBLE
        } else {
            homeMenu.visibility = View.INVISIBLE
        }
    }

    fun showHomeFolder(state: Boolean) {
        if (state) {
            homeFolder.visibility = View.VISIBLE
        } else {
            homeFolder.visibility = View.INVISIBLE
        }
        isFolderOpen = state
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
        var dragAndDropData = DragAndDropData()
        lateinit var dualWallpaper: DualWallpaper

        fun initCompanion() {
            Log.d("COMPANION", "Initialize companion...")
            dualWallpaper = DualWallpaper(mainContext)
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
            showHomeFragment(settingsFragment)
        }

        if (widgetFragment.isVisible) {
            showHomeFragment(widgetFragment)
        }
    }

    fun showHomeFragment(visible: Fragment) {
        gestureLayout.setGesturesOn(true)
        supportFragmentManager
            .beginTransaction()
            .remove(visible)
            .commitNow()
    }

    override fun onResume() {
        onBackPressed()
        super.onResume()
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        setStatusBars()
        setNavBarBackground()
        val dsb = View.DragShadowBuilder(view)
        startDragAndDrop(view, clipData, dsb, null, 0)

        if (isFolderOpen) {
            if (!drawerFragment.isVisible) {
                showHomeFolder(false)
            }
        }

        if (drawerFragment.isVisible) {
            showHomeFragment(drawerFragment)
        }
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        gestureLayout.setGesturesOn(true)
        setStatusBars()
        setNavBarBackground()
        if (drawerFragment.isVisible) {
            showHomeFragment(drawerFragment)
        }
        showHomeFolder(false)
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
        persistGrid(getCurrentHomePagerItem())
    }

    override fun onLongClick(view: View) {
        Log.d(
            TAG,
            "onLongClick registered at display: ID:${windowManager.defaultDisplay.displayId}, Name:${windowManager.defaultDisplay.name}"
        )
        showHomeMenu(true)
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key != null) {
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

            if (key.contains("homeIconsGrid")) {
                homePagerAdapter.notifyDataSetChanged()
            }
            if (key.contains("homeWidgetsGrid")) {
                homePagerAdapter.notifyDataSetChanged()
            }
            if (key == "apps") {
                if (dock != null) {
                    dock.populateDock()
                }
            }

            if (key == "home_grid_pages" || key == "home_grid_columns" || key == "home_grid_rows") {
                homePagerAdapter.notifyDataSetChanged()
                setupHomePageIndicator()
            }
            if (key == "home_widget_color") {
                setupHomePageIndicator()
                homeDelete.setColorFilter(
                    colorPrefToColor(
                        settingsPreferences.getString(
                            "home_widget_color",
                            "White"
                        )
                    )
                )
            }
            if (key == "home_text_color") {
                homePagerAdapter.notifyDataSetChanged()
            }
        }
    }

    fun getCurrentHomePagerItem(): Int {
        return homePager.currentItem
    }

    override fun onAnimationRepeat(p0: Animation?) {
        // Do nothing
    }

    override fun onAnimationEnd(p0: Animation?) {
        showHomeFragment(drawerFragment)
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
        if (setting != null) {
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
        showHomeFragment(widgetFragment)
        if (homePager.startDragAndDrop(clipData, dsb, false, 0)) {
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
        persistGrid(getCurrentHomePagerItem())
    }

    override fun configureWidget(widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo) {
        intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        intent.setComponent(widgetProviderInfo.configure)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        try {
            startActivityForResult(intent, CONFIGURE_WIDGET)
        } catch (e: Exception) {
            createWidget(null)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PERMISSION) {
                configureWidgetData(data)
            }

            if (requestCode == CONFIGURE_WIDGET) {
                createWidget(data)
            }
        } else {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun configureWidgetData(data: Intent? = null) {
        val view =
            homePager.findViewWithTag<View>(getCurrentHomePagerItem())
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (n in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(n) is WidgetContainer) {
                val child = homeIconsTable.getChildAt(n) as WidgetContainer
                if (child.isWaitingForPermission) {
                    child.configureWidget(data)
                }
            }
        }
    }

    fun createWidget(data: Intent?) {
        val view =
            homePager.findViewWithTag<View>(getCurrentHomePagerItem())
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (n in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(n) is WidgetContainer) {
                val child = homeIconsTable.getChildAt(n) as WidgetContainer
                if (child.isWaitingForConfigure) {
                    child.createWidget(data)
                }
            }
        }
    }

    override fun onShowFolder(state: Boolean) {
        showHomeFolder(state)
    }

    override fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder) {
        folderName.text = name
        folderDebug.text = folder.getLaunchInfo().getFolderUniqueId().toString()
        if(settingsPreferences.getBoolean("show_folder_id", false)) {
            folderDebug.visibility = View.VISIBLE
        } else {
            folderDebug.visibility = View.INVISIBLE
        }
        val folderAdapter = FolderAdapter(this as AppCompatActivity, apps, folder)
        folderAdapter.setListener(this)
        folderGrid.adapter = folderAdapter
        folderAdapter.notifyDataSetChanged()

        homeFolderBackground.setOnClickListener {
            showHomeFolder(false)
        }

        folderName.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                folder.setFolderName(folderName.text.toString())
            }
        }

        folderGrid.setOnDragListener { view, dragEvent ->
            if (dragEvent != null) {
                var respondToDrag = false
                try {
                    if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                        respondToDrag = true
                    }
                } catch (e: Exception) {
                    respondToDrag = false
                }

                when (dragEvent.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        if (respondToDrag) {
                            folderGrid.setBackgroundResource(R.drawable.icon_drag_target)
                        }
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (respondToDrag) {
                            folderGrid.setBackgroundColor(enteredColor)
                        }
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        if (respondToDrag) {
                            folderGrid.setBackgroundResource(R.drawable.icon_drag_target)
                        }
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        folderGrid.setBackgroundColor(Color.TRANSPARENT)
                    }
                    DragEvent.ACTION_DROP -> {
                        if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                            val info = MainActivity.dragAndDropData.retrieveLaunchInfo(id)
                            folder.addFolderApp(info)
                            folderAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            true
        }
    }

    override fun onFolderChanged() {
        persistGrid(getCurrentHomePagerItem())
    }
}
