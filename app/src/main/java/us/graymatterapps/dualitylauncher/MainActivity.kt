package us.graymatterapps.dualitylauncher

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat.startDragAndDrop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dual_launch.*
import kotlinx.android.synthetic.main.dual_launch.view.*
import kotlinx.android.synthetic.main.folder.view.*
import kotlinx.android.synthetic.main.fragment_drawer.*
import kotlinx.android.synthetic.main.home_dual_launch.*
import kotlinx.android.synthetic.main.home_folder.*
import kotlinx.android.synthetic.main.home_screen_menu.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import us.graymatterapps.graymatterutils.GrayMatterUtils.getVersionCode
import us.graymatterapps.graymatterutils.GrayMatterUtils.shortToast
import us.graymatterapps.graymatterutils.GrayMatterUtils.showOkDialog

const val PREFS_FILENAME = "us.graymatterapps.dualitylauncher.prefs"
const val REQUEST_PERMISSION = 1
const val CONFIGURE_WIDGET = 2
const val WIDE_SCREENSHOT = 3
const val UNINSTALL = 4
var isFolderOpen = false
var isDualLaunchOpen = false
lateinit var generalContext: Context
var dragWidgetBitmap: Bitmap? = null

class MainActivity : AppCompatActivity(), AppDrawerAdapter.DrawerAdapterInterface,
    SharedPreferences.OnSharedPreferenceChangeListener, Animation.AnimationListener,
    GestureLayout.GestureEvents, Dock.DockInterface,
    HomePagerAdapter.HomeIconsInterface, WidgetFragment.WidgetInterface,
    SettingsDeveloper.DeveloperInterface, WidgetContainer.WidgetInterface,
    FolderAdapter.FolderAdapterInterface, WidgetDB.WidgetDBInterface,
    Replicator.ReplicatorInterface {

    lateinit var homeActivity: MainActivity
    var displayId: Int = 99999

    lateinit var drawerFragment: DrawerFragment
    lateinit var widgetFragment: WidgetFragment
    lateinit var settingsFragment: SettingsFragment
    lateinit var displayManager: DisplayManager
    lateinit var homePagerAdapter: HomePagerAdapter
    lateinit var dock: Dock
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 20)

    val TAG = javaClass.simpleName

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generalContext = this
        homeActivity = this
        displayId = this.display!!.displayId
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        if (isMainDisplay()) {
            postUpdateCheck()
        }
        widgetDB.setListener(this, displayId)

        replicator.register(displayId, this)
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            settingsPreferences.unregisterOnSharedPreferenceChangeListener(this)
        } catch (e: Exception) {
            // Do nothing
        }
        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)

        drawerFragment = DrawerFragment()
        widgetFragment = WidgetFragment()
        settingsFragment = SettingsFragment()

        setContentView(R.layout.activity_main)
        if (displayId == 0) {
            homePagerMain = homePager
            mainScreen = frameLayout
            mainContext = this
        }
        if (displayId == 1) {
            homePagerDual = homePager
            dualScreen = frameLayout
        }
        setStatusBars()
        setNavBarBackground()
        gestureLayout.setListener(this)
        gestureLayout.setGesturesOn(true)
        setupHomeMenu()
        showHomeMenu(false)
        showHomeFolder(false)
        showHomeDualLaunch(false)

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

        dock = Dock(this, null)
        dockContainer.addView(dock)
        dock.depersistDock()
        dock.populateDock()
        dock.setListener(this)

        homePager.offscreenPageLimit = 5
        homePagerAdapter = HomePagerAdapter(this, frameLayout)
        homePager.adapter = homePagerAdapter
        homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                Log.d(TAG, "onPageScrollStateChanged() state:$state")
                super.onPageScrollStateChanged(state)
                setupHomePageIndicator()

                if (dualityLauncherApplication.arePagersInitialized()) {
                    if (settingsPreferences.getBoolean("linked_viewpager", false)) {
                        val lastPage =
                            Integer.parseInt(
                                settingsPreferences.getString(
                                    "home_grid_pages",
                                    "1"
                                )
                            ) - 1
                        if (lastPage > 0) {
                            if (displayId == 0) {
                                val currentPage = homePager.currentItem
                                if (currentPage == 0) {
                                    homePagerDual.setCurrentItem(lastPage, true)
                                } else {
                                    homePagerDual.setCurrentItem(currentPage - 1, true)
                                }
                            }
                            if (displayId == 1) {
                                val currentPage = homePager.currentItem
                                if (currentPage == lastPage) {
                                    homePagerMain.setCurrentItem(0, true)
                                } else {
                                    homePagerMain.setCurrentItem(currentPage + 1, true)
                                }
                            }
                        }
                    }
                }
            }
        })
        homePagerAdapter.setListener(this)
        if (displayId == 1 && settingsPreferences.getBoolean("linked_viewpager", false)) {
            val lastPage =
                Integer.parseInt(
                    settingsPreferences.getString(
                        "home_grid_pages",
                        "1"
                    )
                ) - 1
            homePager.setCurrentItem(lastPage, false)
        }

        setupHomePageIndicator()

        homeDelete.setColorFilter(
            settingsPreferences.getInt(
                "home_widget_color",
                Color.WHITE
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
                                widgetDB.deleteWidget(widgetInfo.getAppWidgetId())
                            }
                            if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                                val id = dragEvent.clipData.getItemAt(0).text.toString()
                                val info = dragAndDropData.retrieveLaunchInfo(id)
                                if (info.getType() == LaunchInfo.FOLDER) {
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

    override fun onDestroy() {
        super.onDestroy()
        replicator.deregister(displayId)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        settingsPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun setupHomePageIndicator() {
        homePageIndicator.removeAllViews()
        var params = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val filterColor = settingsPreferences.getInt(
            "home_widget_color",
            -1
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
        homePagerAdapter.lock.lock()
        Log.d(TAG, "persistGrid()")
        var homeIconsGrid = HomeIconsGrid()
        var homeWidgetsGrid = HomeWidgetsGrid()
        val view = frameLayout.findViewWithTag<View>(position)
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
            if (homeIconsTable.getChildAt(n) is DualLaunch) {
                val child = homeIconsTable.getChildAt(n) as DualLaunch
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
        editor.putString("homeWidgetsGrid" + displayId + ":" + position, saveItJson)
        editor.apply()
        homePagerAdapter.lock.unlock()
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
        buttonActionWideshot.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                wideScreenshot()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WIDE_SCREENSHOT
                )
            }
        }
        homeMenuBackground.setOnClickListener {
            showHomeMenu(false)
        }
    }

    private fun wideScreenshot() {
        Log.d(TAG, "wideScreenshot()")
        showHomeMenu(false)
        Thread(Runnable {
            Thread.sleep(500)
            this@MainActivity.runOnUiThread {
                dualityLauncherApplication.wideShot()
            }
        }).start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WIDE_SCREENSHOT -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    wideScreenshot()
                }
            }
        }
    }

    fun showHomeMenu(state: Boolean) {
        if (state) {
            homeMenu.visibility = View.VISIBLE
            val animation =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.grow_fade_in_center)
            homeMenu.startAnimation(animation)
        } else {
            homeMenu.visibility = View.INVISIBLE
            val animation =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.shrink_fade_out_center)
            homeMenu.startAnimation(animation)
        }
    }

    fun showHomeFolder(state: Boolean) {
        if (state) {
            homeFolder.visibility = View.VISIBLE
            val animation =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.grow_fade_in_center)
            homeFolder.startAnimation(animation)
        } else {
            gestureLayout.setDialogOpen(false)
            homeFolder.visibility = View.INVISIBLE
            val animation =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.shrink_fade_out_center)
            homeFolder.startAnimation(animation)
        }
        isFolderOpen = state
    }

    fun showHomeDualLaunch(state: Boolean) {
        if (state) {
            homeDualLaunch.visibility = View.VISIBLE
            val animation =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.grow_fade_in_center)
            homeDualLaunch.startAnimation(animation)
        } else {
            gestureLayout.setDialogOpen(false)
            homeDualLaunch.visibility = View.INVISIBLE
            val animation =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.shrink_fade_out_center)
            homeDualLaunch.startAnimation(animation)
        }
        isDualLaunchOpen = state
    }

    @SuppressLint("NewApi")
    fun isMainDisplay(): Boolean {
        val displays = displayManager.displays
        val currentDisplay = this.display!!.displayId
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
        var color = settingsPreferences.getInt(
            "status_background",
            Color.BLACK
        )
        window.statusBarColor = color
        if (settingsPreferences.getBoolean("status_light", true)) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    fun setNavBarBackground() {
        var color = settingsPreferences.getInt("nav_background", Color.BLACK)
        window.navigationBarColor = color
    }

    fun closeAppDrawer() {
        gestureLayout.setDrawerOpen(false)
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.drawer_slide_down)
        animation.setAnimationListener(this)
        fragmentFrame.startAnimation(animation)
        //val animation2 = AnimationUtils.loadAnimation(this@MainActivity, R.anim.home_slide_down)
        //gestureLayout.startAnimation(animation2)
    }

    override fun onBackPressed() {
        if (homeFolder.visibility == View.VISIBLE && !drawerFragment.isVisible) {
            showHomeFolder(false)
        }

        if (homeDualLaunch.visibility == View.VISIBLE && !drawerFragment.isVisible) {
            showHomeDualLaunch(false)
        }

        if (drawerFragment.isVisible) {
            closeAppDrawer()
        }

        if (settingsFragment.isVisible) {
            showHomeFragment(settingsFragment)
        }

        if (widgetFragment.isVisible) {
            showHomeFragment(widgetFragment)
        }

        resetResize()
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

    override fun onUninstall(launchInfo: LaunchInfo) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
        intent.setData(Uri.parse("package:" + launchInfo.getPackageName()))
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        startActivityForResult(intent, UNINSTALL)
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
            Log.d(TAG, "onSharedPreferenceChanged() $key")
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

            if (key == "apps") {
                dock.populateDock()
            }

            if (key == "home_grid_pages" || key == "home_grid_columns" || key == "home_grid_rows") {
                homePagerAdapter.lock.lock()
                homePagerAdapter.notifyDataSetChanged()
                homePagerAdapter.lock.unlock()
                setupHomePageIndicator()
            }
            if (key == "home_widget_color") {
                setupHomePageIndicator()
                homeDelete.setColorFilter(
                    settingsPreferences.getInt(
                        "home_widget_color",
                        -1
                    )
                )
            }
            if (key == "home_text_color") {
                homePagerAdapter.lock.lock()
                homePagerAdapter.notifyDataSetChanged()
                homePagerAdapter.lock.unlock()
            }
            if (key == "home_text_shadow_color") {
                homePagerAdapter.lock.lock()
                homePagerAdapter.notifyDataSetChanged()
                homePagerAdapter.lock.unlock()
            }
            if (key == "home_icon_padding") {
                homePagerAdapter.lock.lock()
                homePagerAdapter.notifyDataSetChanged()
                homePagerAdapter.lock.unlock()
            }
            if (key == "home_text_size") {
                homePagerAdapter.lock.lock()
                homePagerAdapter.notifyDataSetChanged()
                homePagerAdapter.lock.unlock()
            }
            if (key == "widget_info") {
                showWidgets()
            }
            if (key == "notifyDataSetChanged") {
                homePagerAdapter.lock.lock()
                homePagerAdapter.notifyDataSetChanged()
                homePagerAdapter.lock.unlock()
                dock.depersistDock()
                dock.populateDock()
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
        dock.clearSearchFocus()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentFrame, drawerFragment, "drawer")
            .commitNowAllowingStateLoss()
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.drawer_slide_up)
        fragmentFrame.startAnimation(animation)
        var color = settingsPreferences.getInt(
            "app_drawer_background",
            Color.BLACK
        )

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
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
    }

    override fun onSwipeUp() {
        if (!drawerFragment.isVisible) {
            showDrawerFragment()
            gestureLayout.setDrawerOpen(true)
        }
    }

    @SuppressLint("WrongConstant")
    override fun onSwipeDown() {
        if (drawerFragment.isVisible) {
            if (drawerFragment.gridLayoutManager.findFirstVisibleItemPosition() == 0) {
                closeAppDrawer()
            }
        } else {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManager.getMethod("expandNotificationsPanel")
            expand.invoke(statusBarService)
        }
    }

    fun setWideMode() {
        val winManager = Class.forName("android.view.WindowManager")
        val switchWideScreenMode = winManager.getMethod("switchWideScreenMode")
        switchWideScreenMode.invoke(true)
    }

    override fun onAddWidget(clipData: ClipData, view: View) {
        val dsb = WidgetDragShadowBuilder(view)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.layout(view.left, view.top, view.right, view.bottom)
        view.draw(canvas)
        dragWidgetBitmap = bitmap
        showHomeFragment(widgetFragment)
        if (homePager.startDragAndDrop(clipData, dsb, false, 0)) {
            Log.d(TAG, "onAddWidget() startDragAndDrop successful")
        } else {
            Log.d(TAG, "onAddWidget() startDragAndDrop failed")
        }
    }

    fun postUpdateCheck() {
        val previousVersion = prefs.getInt("previousVersion", 0)
        var editor = prefs.edit()

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

        editor = settingsPreferences.edit()

        if (previousVersion < 16) {
            editor.remove("dock_background_color")
            editor.remove("home_widget_color")
            editor.remove("home_text_color")
            editor.remove("app_drawer_background")
            editor.remove("app_drawer_text")
            editor.remove("status_background")
            editor.remove("nav_background")
        }
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

    override fun showWidgets() {
        if (settingsPreferences.getBoolean("widget_info", false)) {
            widgetList.visibility = View.VISIBLE
            val widgetTags = ArrayList<String>()
            widgetDB.widgets.forEach {
                widgetTags.add("${it.widgetId}:${it.widgetProviderInfo.provider.shortClassName}")
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, widgetTags)
            widgetList.adapter = adapter
            adapter.notifyDataSetChanged()
        } else {
            widgetList.visibility = View.INVISIBLE
        }
    }

    fun needPermissionToBind(
        widgetId: Int,
        widgetProviderInfo: AppWidgetProviderInfo
    ) {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetProviderInfo.provider)
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,
                appWidgetManager.getAppWidgetOptions(widgetId)
            )
        }
        startActivityForResult(intent, REQUEST_PERMISSION)
    }

    override fun onWidgetChanged() {
        try {
            persistGrid(getCurrentHomePagerItem())
        } catch (e: Exception) {
            Log.d(TAG, "onWidgetChanged() persistGrid() failed!")
        }
    }

    override fun updateWidgets(widgetInfo: AppWidgetProviderInfo) {
        val intent = Intent(applicationContext, widgetInfo.provider.className.javaClass)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(
                applicationContext,
                widgetInfo.provider.className.javaClass
            )
        )
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        this.sendBroadcast(intent)
    }

    private fun configureWidget(
        widgetId: Int,
    ) {
        Log.d(TAG, "configureWidget()")
        if (widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].widgetProviderInfo.configure != null) {
            intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component =
                widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].widgetProviderInfo.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = displayId
            try {
                startActivityForResult(intent, CONFIGURE_WIDGET, options.toBundle())
            } catch (e: Exception) {
                buildWidget(widgetId)
            }
        } else {
            buildWidget(widgetId)
        }
    }

    override fun initializeWidget(
        hostView: AppWidgetHostView,
        widgetId: Int,
        widgetProviderInfo: AppWidgetProviderInfo
    ) {
        Log.d(TAG, "initializeWidget()")
        if (appWidgetManager.getAppWidgetInfo(widgetId) != null) {
            buildWidget(widgetId)
        } else {
            val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(
                widgetId,
                widgetProviderInfo.provider
            )
            if (canBind) {
                configureWidget(widgetId)
            } else {
                needPermissionToBind(widgetId, widgetProviderInfo)
            }
        }
    }

    private fun buildWidget(widgetId: Int) {
        Log.d(TAG, "buildWidget()")
        widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].appWidgetHostView =
            appWidgetHost.createView(
                applicationContext,
                widgetId,
                appWidgetManager.getAppWidgetInfo(widgetId)
            )
        widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].appWidgetHostView.setAppWidget(
            widgetId,
            appWidgetManager.getAppWidgetInfo(widgetId)
        )
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].appWidgetHostView.updateAppWidgetOptions(
            options
        )
        widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].initialized = true
        widgetDB.widgets[widgetDB.getWidgetIndex(widgetId)].widgetContainer.addWidgetView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PERMISSION) {
                val extras = data!!.extras
                configureWidget(extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID))
            }

            if (requestCode == CONFIGURE_WIDGET) {
                val extras = data!!.extras
                val widgetId = extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)
                buildWidget(widgetId)
            }

            if (requestCode == UNINSTALL) {
                shortToast(this, "Package uninstalled")
            }
        } else {
            if (requestCode == UNINSTALL) {
                shortToast(this, "Package uninstall failed")
            }
            try {
                val extras = data!!.extras
                val widgetId = extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)
                appWidgetHost.deleteAppWidgetId(widgetId)
            } catch (e: Exception) {
                Log.d(TAG, "onActivityResult() Could not deleteAppWidgetId after bad resultCode!")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onShowFolder(state: Boolean) {
        showHomeFolder(state)
    }

    override fun onShowDualLaunch(state: Boolean) {
        showHomeDualLaunch(state)
    }

    override fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder) {
        gestureLayout.setDialogOpen(true)
        val textColor = settingsPreferences.getInt("folder_text", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("folder_text_shadow", Color.BLACK)
        folderName.text = name
        folderName.setTextColor(textColor)
        folderName.setShadowLayer(6F, 0F, 0F, textShadowColor)
        folderWindow.backgroundTintList =
            ColorStateList.valueOf(settingsPreferences.getInt("folder_background", Color.BLACK))
        folderDebug.text = folder.getLaunchInfo().getFolderUniqueId().toString()
        if (settingsPreferences.getBoolean("show_folder_id", false)) {
            folderDebug.visibility = View.VISIBLE
        } else {
            folderDebug.visibility = View.INVISIBLE
        }
        val folderAdapter = FolderAdapter(this, apps, folder)
        folderAdapter.setListener(this)
        folderGrid.adapter = folderAdapter
        folderAdapter.notifyDataSetChanged()

        homeFolderBackground.setOnClickListener {
            if (drawerFragment.isVisible) {
                // Do nothing
            } else {
                showHomeFolder(false)
                folder.verifyApps()
            }
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
                            // Do nothing
                        }
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (respondToDrag) {
                            folderGrid.setBackgroundResource(R.drawable.icon_drag_target)
                        }
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        if (respondToDrag) {
                            folderGrid.setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        folderGrid.setBackgroundColor(Color.TRANSPARENT)
                    }
                    DragEvent.ACTION_DROP -> {
                        if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                            val info = dragAndDropData.retrieveLaunchInfo(id)
                            folder.addFolderItem(info)
                            folderAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            true
        }
    }

    override fun onSetupDualLaunch(
        apps: ArrayList<LaunchInfo>,
        name: Editable,
        dualLaunch: DualLaunch
    ) {
        gestureLayout.setDialogOpen(true)
        val textColor = settingsPreferences.getInt("folder_text", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("folder_text_shadow", Color.BLACK)
        dualLaunchName.text = name
        dualLaunchName.setTextColor(textColor)
        dualLaunchName.setShadowLayer(6F, 0F, 0F, textShadowColor)
        dualLaunchWindow.backgroundTintList =
            ColorStateList.valueOf(settingsPreferences.getInt("folder_background", Color.BLACK))

        val iconParams = LinearLayout.LayoutParams(180, 170)
        val dualLaunchLeft = Icon(this, null, false, 0)
        dualLaunchLeft.icon.layoutParams = iconParams
        dualLaunchLeft.setLaunchInfo(apps[0])
        dualLaunchLeft.setBlankOnDrag(true)
        dualLaunchLeft.setDockIcon(true)
        dualLaunchLeft.setDragTarget(true)
        dualLaunchLeft.setDualLaunch(true)
        dualLaunchLeft.label.setTextColor(textColor)
        dualLaunchLeft.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
        val dualLaunchRight = Icon(this, null, false, 0)
        dualLaunchRight.icon.layoutParams = iconParams
        dualLaunchRight.setLaunchInfo(apps[1])
        dualLaunchRight.setBlankOnDrag(true)
        dualLaunchRight.setDockIcon(true)
        dualLaunchRight.setDragTarget(true)
        dualLaunchRight.setDualLaunch(true)
        dualLaunchRight.label.setTextColor(textColor)
        dualLaunchRight.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
        dualLaunchIconsLayout.removeAllViews()
        dualLaunchIconsLayout.addView(dualLaunchLeft)
        dualLaunchIconsLayout.addView(dualLaunchRight)

        buttonDLOk.setOnClickListener {
            dualLaunch.addFirstApp(dualLaunchLeft.getLaunchInfo())
            dualLaunch.addSecondApp(dualLaunchRight.getLaunchInfo())
            showHomeDualLaunch(false)
        }

        buttonSwap.setOnClickListener {
            val launchRight = dualLaunchRight.getLaunchInfo().copy()
            val launchLeft = dualLaunchLeft.getLaunchInfo().copy()
            dualLaunch.addFirstApp(launchRight)
            dualLaunch.addSecondApp(launchLeft)
            dualLaunchLeft.setLaunchInfo(apps[0])
            dualLaunchRight.setLaunchInfo(apps[1])
        }

        homeDualLaunchBackground.setOnClickListener {
            if (drawerFragment.isVisible) {
                // Do nothing
            } else {
                dualLaunch.addFirstApp(dualLaunchLeft.getLaunchInfo())
                dualLaunch.addSecondApp(dualLaunchRight.getLaunchInfo())
                showHomeDualLaunch(false)
            }
        }

        dualLaunchName.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                dualLaunch.setDualLaunchName(dualLaunchName.text.toString())
                persistGrid(getCurrentHomePagerItem())
            }
        }
    }

    override fun onFolderChanged() {
        persistGrid(getCurrentHomePagerItem())
    }

    override fun resetResize() {
        val view = frameLayout.findViewWithTag<View>(getCurrentHomePagerItem())
        if (view != null) {
            val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
            for (n in 0 until homeIconsTable.childCount) {
                if (homeIconsTable.getChildAt(n) is WidgetContainer) {
                    val child = homeIconsTable.getChildAt(n) as WidgetContainer
                    child.resetResize()
                }
            }
        }
        dock.dockSearchWidget.clearSearchFocus()
    }

    override fun logRecents() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - android.os.SystemClock.uptimeMillis(),
            time
        )
        stats.forEach {
            Log.d(TAG, it.packageName)
        }
    }

    override fun removeWidgets(leavePager: Boolean) {
        widgetDB.widgets.clear()
        widgetDB.sizes.clear()
        AppWidgetHost.deleteAllHosts()
        if (!leavePager) {
            homePagerAdapter.lock.lock()
            homePagerAdapter.notifyDataSetChanged()
            homePagerAdapter.lock.unlock()
        }
    }

    override fun deleteViews(page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        if (view != null) {
            val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
            for (i in 0 until homeIconsTable.childCount) {
                val child = homeIconsTable.getChildAt(i)
                if (child != null) {
                    val params = child.layoutParams as HomeLayout.LayoutParams
                    if (params.row == row && params.column == column) {
                        homeIconsTable.removeView(child)
                    }
                }
            }
            homePagerAdapter.lock.unlock()
            persistGrid(page)
        }
    }

    override fun addIcon(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        if (view != null) {
            val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
            val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
            val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
            var icon = Icon(this, null, false, page)
            var iconParams = HomeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            iconParams.row = row
            iconParams.column = column
            iconParams.rowSpan = 1
            iconParams.columnSpan = 1
            icon.layoutParams = iconParams
            icon.label.maxLines = 1
            icon.label.setTextColor(textColor)
            icon.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
            icon.setListener(homePagerAdapter)
            icon.setBlankOnDrag(true)
            icon.setDockIcon(false)
            icon.setLaunchInfo(
                launchInfo.getActivityName(),
                launchInfo.getPackageName(),
                launchInfo.getUserSerial()
            )
            homeIconsTable.addView(icon, iconParams)
            homePagerAdapter.lock.unlock()
            persistGrid(page)
        }
    }

    override fun changeIcon(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (i in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(i) is Icon) {
                val child = homeIconsTable.getChildAt(i) as Icon
                if (child != null) {
                    val params = child.layoutParams as HomeLayout.LayoutParams
                    if (params.row == row && params.column == column) {
                        child.replicate = false
                        child.setBlankOnDrag(true)
                        child.setLaunchInfo(launchInfo)
                    }
                }
            }
        }
        homePagerAdapter.lock.unlock()
        persistGrid(page)
    }

    override fun addFolder(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
        var folder = Folder(
            this,
            null,
            launchInfo.getFolderName(),
            launchInfo,
            false,
            page
        )
        folder.setListener(homePagerAdapter)
        var folderParams = HomeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        folderParams.row = row
        folderParams.column = column
        folderParams.rowSpan = 1
        folderParams.columnSpan = 1
        folder.layoutParams = folderParams
        folder.folderLabel.setTextColor(textColor)
        folder.folderLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
        homeIconsTable.addView(folder, folderParams)
        homePagerAdapter.lock.unlock()
        persistGrid(page)
    }

    override fun addDualLaunch(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
        var dualLaunch = DualLaunch(
            this,
            null,
            launchInfo.getDualLaunchName(),
            launchInfo,
            false,
            page
        )
        dualLaunch.setListener(homePagerAdapter)
        var dualLaunchParams = HomeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dualLaunchParams.row = row
        dualLaunchParams.column = column
        dualLaunchParams.rowSpan = 1
        dualLaunchParams.columnSpan = 1
        dualLaunch.layoutParams = dualLaunchParams
        dualLaunch.dualLaunchLabel.setTextColor(textColor)
        dualLaunch.dualLaunchLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
        homeIconsTable.addView(dualLaunch, dualLaunchParams)
        homePagerAdapter.lock.unlock()
        persistGrid(page)
    }

    override fun changeFolder(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (i in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(i) is Folder) {
                val child = homeIconsTable.getChildAt(i) as Folder
                if (child != null) {
                    val params = child.layoutParams as HomeLayout.LayoutParams
                    if (params.row == row && params.column == column) {
                        child.replicate = false
                        child.setLaunchInfo(launchInfo)
                    }
                }
            }
        }
        homePagerAdapter.lock.unlock()
        persistGrid(page)
    }

    override fun changeDualLaunch(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        homePagerAdapter.lock.lock()
        val view = frameLayout.findViewWithTag<View>(page)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (i in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(i) is DualLaunch) {
                val child = homeIconsTable.getChildAt(i) as DualLaunch
                if (child != null) {
                    val params = child.layoutParams as HomeLayout.LayoutParams
                    if (params.row == row && params.column == column) {
                        child.replicate = false
                        child.setLaunchInfo(launchInfo)
                    }
                }
            }
        }
        homePagerAdapter.lock.unlock()
        persistGrid(page)
    }
}
