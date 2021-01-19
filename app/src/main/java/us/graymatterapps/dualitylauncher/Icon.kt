package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.graphics.ColorUtils
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.setPadding
import kotlinx.android.synthetic.main.dual_launch.view.*
import kotlinx.android.synthetic.main.folder.view.*
import us.graymatterapps.graymatterutils.GrayMatterUtils

class Icon(
    private val parentActivity: MainActivity,
    attrs: AttributeSet?,
    activityInfo: String,
    packageInfo: String,
    userSerial: Long,
    isDragTarget: Boolean,
    isBlankOnDrag: Boolean,
    var replicate: Boolean = false,
    var page: Int
) : LinearLayout(parentActivity, attrs) {

    constructor(con: MainActivity, attrs: AttributeSet?, replicate: Boolean, page: Int) : this(
        con,
        attrs,
        "",
        "",
        0,
        true,
        false,
        replicate,
        page
    )

    constructor(
        con: MainActivity,
        attrs: AttributeSet?,
        activityInfo: String,
        packageInfo: String,
        userSerial: Long
    ) : this(con, attrs, activityInfo, packageInfo, userSerial, true, false, false, 0)

    private lateinit var listener: IconInterface
    lateinit var parentLayout: ViewGroup
    var iconLayout: LinearLayout
    var icon: ImageView
    var label: TextView
    private var launchInfo = LaunchInfo()
    private var dragTarget = true
    private var blankOnDrag = false
    private var isDockIcon = false
    private var isDualLaunchEditWindow = false
    private val touchSlop = 10
    private val longClickTime = android.view.ViewConfiguration.getLongPressTimeout()
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 20)
    private val pulseAnim = AnimationUtils.loadAnimation(context, R.anim.pulse_alpha)
    private lateinit var menu: PopupMenu
    private var isPopupMenuVisible: Boolean = false
    private var downTime: Long = 0
    private var padding: Int = 5
    val TAG = javaClass.simpleName

    init {
        inflate(context, R.layout.icon, this)

        dragTarget = isDragTarget

        if (attrs != null) {
            setupAttrs(attrs)
        }

        iconLayout = findViewById(R.id.iconLayout)
        icon = findViewById(R.id.iconImage)
        label = findViewById(R.id.label)

        blankOnDrag = isBlankOnDrag

        launchInfo.setActivityName(activityInfo)
        launchInfo.setPackageName(packageInfo)
        launchInfo.setUserSerial(userSerial)

        setupIcon()

        if (dragTarget) {
            iconLayout.setOnDragListener { view, dragEvent ->
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
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundResource(R.drawable.icon_drag_target)
                            }
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundColor(Color.TRANSPARENT)
                                //iconLayout.startAnimation(pulseAnim)
                            }
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            iconLayout.setBackgroundColor(Color.TRANSPARENT)
                            //iconLayout.clearAnimation()
                        }
                        DragEvent.ACTION_DROP -> {
                            if (respondToDrag) {
                                if (dragEvent.clipDescription.label.toString().equals("launchInfo")
                                ) {
                                    if (!isDockIcon) {
                                        if (launchInfo.getActivityName() != "") {
                                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                                            val info = dragAndDropData.retrieveLaunchInfo(id)
                                            convertToFolder(info)
                                        } else {
                                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                                            launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                                            if (launchInfo.getType() == LaunchInfo.ICON) {
                                                if (this.parent is HomeLayout) {
                                                    replicate = false
                                                    val params =
                                                        this.layoutParams as HomeLayout.LayoutParams
                                                    replicator.changeIcon(
                                                        parentActivity.displayId,
                                                        launchInfo,
                                                        page,
                                                        params.row,
                                                        params.column
                                                    )
                                                }
                                                setupIcon()
                                            } else {
                                                convertToFolder(launchInfo)
                                            }
                                        }
                                    } else {
                                        val id = dragEvent.clipData.getItemAt(0).text.toString()
                                        launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                                        if (launchInfo.getType() == LaunchInfo.ICON) {
                                            this.launchInfo = launchInfo
                                            setupIcon()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                true
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isScrolling) {
            if (event != null) {
                Log.d(TAG, "onTouchEvent() ${MotionEvent.actionToString(event.action)}")
                if (downTime > 0 && System.currentTimeMillis() - downTime > longClickTime) {
                    Log.d(TAG, "Downtime > longClickTime")
                    downTime = 0
                    if (!isDualLaunchEditWindow) {
                        showPopupMenu()
                    }
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTime = System.currentTimeMillis()
                        swiping = false
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (System.currentTimeMillis() - downTime < longClickTime) {
                            if (!isDualLaunchEditWindow) {
                                if(!swiping) {
                                    launch()
                                }
                            }
                        }
                        downTime = 0
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isPopupMenuVisible) {
                            if (event.historySize != 0) {
                                val distance = GrayMatterUtils.getDistance(
                                    event.getHistoricalX(0),
                                    event.getHistoricalY(0),
                                    event.getX(),
                                    event.getY()
                                )
                                Log.d(TAG, "distance = $distance, touchSlop = $touchSlop")
                                if (distance > touchSlop) {
                                    swiping = true
                                    downTime = 0
                                    menu.dismiss()
                                    startDragging()
                                }
                            }
                        }
                        return true
                    }
                }
            }
            return true
        } else {
            downTime = 0
        }
        return false
    }

    private fun showPopupMenu() {
        if (launchInfo.getActivityName() != "") {
            GrayMatterUtils.vibrate(parentActivity, 50)
            menu = PopupMenu(parentActivity, this)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                menu.setForceShowIcon(true)
            }

            val menuItemAppInfo = menu.menu.add(1, 1, 1, "App Info")
            menuItemAppInfo.setIcon(R.drawable.ic_app_info)
            menuItemAppInfo.setOnMenuItemClickListener {
                appList.launchAppInfo(parentActivity, this.launchInfo, parentActivity.displayId)
                true
            }

            if (this.parent is HomeLayout || this.parent is TableRow || this.parent is LinearLayout) {
                val menuItemRemove = menu.menu.add(1, 2, 1, "Remove")
                menuItemRemove.setIcon(R.drawable.ic_remove)
                menuItemRemove.setOnMenuItemClickListener {
                    removeIcon()
                    true
                }
            }

            val menuItemUninstall = menu.menu.add(1, 3, 1, "Uninstall")
            menuItemUninstall.setIcon(R.drawable.ic_uninstall)
            menuItemUninstall.setOnMenuItemClickListener {
                listener.onUninstall(launchInfo)
                true
            }

            val menuItemLaunchOtherScreen = menu.menu.add(1, 4, 1, "Launch on other display")
            menuItemLaunchOtherScreen.setIcon(R.drawable.ic_smartphone)
            menuItemLaunchOtherScreen.setOnMenuItemClickListener {
                appList.launchPackageOtherDisplay(
                    parentActivity,
                    launchInfo,
                    parentActivity.displayId
                )
                true
            }

            if (::parentLayout.isInitialized) {
                val menuItemCreateDL = menu.menu.add(1, 5, 1, "Create dual launch")
                menuItemCreateDL.setIcon(R.drawable.ic_dual_launch)
                menuItemCreateDL.setOnMenuItemClickListener {
                    convertToDualLaunch()
                    true
                }
            }

            var itemText = "Set as work app"
            if (appList.isManualWorkApp(launchInfo)) {
                itemText = "Set as non-work app"
            }
            val menuItemManualWorkApp = menu.menu.add(1, 6, 1, itemText)
            menuItemManualWorkApp.setIcon(R.drawable.ic_work)
            menuItemManualWorkApp.setOnMenuItemClickListener {
                if (appList.isManualWorkApp(launchInfo)) {
                    appList.desetAsManualWorkApp(launchInfo)
                } else {
                    appList.setAsManualWorkApp(launchInfo)
                }
                listener.onReloadAppDrawer()
                true
            }

            val shortcuts = appList.getAppShortcuts(launchInfo.getPackageName())
            var menuItemID: Int = 100
            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    val menuItemShortcut = menu.menu.add(1, menuItemID, 1, shortcut.label)
                    val shortcutIcon = appList.loadShortcutIcon(shortcut.shortcutInfo)
                    if (shortcutIcon != null) {
                        val icon = GrayMatterUtils.resizeDrawable(parentActivity, shortcutIcon, 63)
                        menuItemShortcut.setIcon(icon)
                    }
                    menuItemShortcut.setOnMenuItemClickListener {
                        appList.startShortcut(shortcut)
                        menuItemID++
                        true
                    }
                }
            }

            menu.setOnDismissListener {
                isPopupMenuVisible = false
            }
            isPopupMenuVisible = true
            menu.show()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this.parent is HomeLayout) {
            parentLayout = this.parent as HomeLayout
            if (replicate) {
                val params = this.layoutParams as HomeLayout.LayoutParams
                replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
                replicator.addIcon(
                    parentActivity.displayId,
                    launchInfo,
                    page,
                    params.row,
                    params.column
                )
            }
        }
        if (this.parent is LinearLayout) {
            parentLayout = this.parent as LinearLayout
        }
    }

    private fun launch() {
        if (launchInfo.getActivityName() != "") {
            if (launchInfo.getActivityName() == "allapps") {
                listener.onOpenDrawer()
            } else {
                listener.onLaunch(launchInfo, parentActivity.displayId)
            }
        }
    }

    private fun startDragging() {
        if (launchInfo.getActivityName() != "") {
            val id = System.currentTimeMillis().toString()
            if (::parentLayout.isInitialized) {
                if (parentLayout is HomeLayout) {
                    val params = this.layoutParams as HomeLayout.LayoutParams
                    launchInfo.setLastXY(params.column, params.row)
                }
            }
            val passedLaunchInfo = launchInfo.copy()
            dragAndDropData.addLaunchInfo(passedLaunchInfo, id)
            var clipData = ClipData.newPlainText("launchInfo", id)
            listener.onDragStarted(this, clipData)

            removeIcon()
        }
    }

    private fun setupAttrs(attrs: AttributeSet) {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.Icon, 0, 0)
        dragTarget = array.getBoolean(R.styleable.Icon_dragTarget, true)
        blankOnDrag = array.getBoolean(R.styleable.Icon_blankOnDrag, false)
    }

    fun setupIcon() {
        if (launchInfo.getActivityName() != "") {
            icon.setImageDrawable(appList.getIcon(launchInfo))
            label.text = appList.getLabel(launchInfo)
            if (replicate) {
                val params = this.layoutParams as HomeLayout.LayoutParams
                replicator.addIcon(
                    parentActivity.displayId,
                    launchInfo,
                    page,
                    params.row,
                    params.column
                )
            }
        } else {
            icon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            label.text = ""
        }
        if (isDockIcon) {
            parentActivity.dock.persistDock()
        }
    }

    fun removeIcon() {
        if (blankOnDrag) {
            launchInfo.setActivityName("")
            launchInfo.setPackageName("")
            launchInfo.setUserSerial(0)
            icon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            label.text = ""
        } else {
            if (this.parent is HomeLayout) {
                convertToEmpty()
            }
        }
        if (isDockIcon) {
            parentActivity.dock.persistDock()
        }
        if (this.parent is HomeLayout) {
            val params = this.layoutParams as HomeLayout.LayoutParams
            replicator.changeIcon(
                parentActivity.displayId,
                launchInfo,
                page,
                params.row,
                params.column
            )
        }
        if (this.parent is LinearLayout) {
            listener.onRemoveFromFolder(launchInfo)
        }

    }

    fun convertToEmpty() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.removeView(this)
        parentActivity.persistGrid(page)
    }

    fun setListener(iconInterface: IconInterface) {
        listener = iconInterface
    }

    fun setLaunchInfo(appActivityName: String, packageName: String, userSerial: Long) {
        launchInfo.setActivityName(appActivityName)
        launchInfo.setPackageName(packageName)
        launchInfo.setUserSerial(userSerial)
        setupIcon()
    }

    fun setLaunchInfo(launchInfos: LaunchInfo) {
        launchInfo = launchInfos
        setupIcon()
    }

    fun getLaunchInfo(): LaunchInfo {
        return launchInfo
    }

    fun setPadding(padSize: Int) {
        padding = padSize
        icon.setPadding(padding)
    }

    fun setBlankOnDrag(state: Boolean) {
        blankOnDrag = state
    }

    fun setDragTarget(state: Boolean) {
        dragTarget = state
    }

    fun setDockIcon(state: Boolean) {
        isDockIcon = state
    }

    fun setDualLaunch(state: Boolean) {
        isDualLaunchEditWindow = state
    }

    fun convertToFolder(info: LaunchInfo) {
        val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
        val textSize = settingsPreferences.getInt("home_text_size", 14)
        var folder: Folder
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (info.getType() == LaunchInfo.ICON) {
            folder = Folder(
                parentActivity,
                null,
                parentActivity.getString(R.string.new_folder),
                null,
                true,
                page
            )
            folder.addFolderItem(launchInfo)
            folder.addFolderItem(info)
        } else {
            folder = Folder(parentActivity, null, info.getFolderName(), info, true, page)
        }
        folder.layoutParams = params
        folder.folderLabel.setTextColor(textColor)
        folder.folderLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
        folder.folderLabel.textSize = textSize.toFloat()
        folder.setListener(parentActivity.homePagerAdapter as Folder.FolderInterface)
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.addView(folder)
        parentLayout.removeView(this)
        parentActivity.persistGrid(page)
    }

    fun convertToDualLaunch() {
        val dualLaunch = DualLaunch(parentActivity, null, "New Dual Launch", null, true, page)
        dualLaunch.addFirstApp(getLaunchInfo())
        val params = this.layoutParams as HomeLayout.LayoutParams
        dualLaunch.layoutParams = params
        dualLaunch.setListener(parentActivity.homePagerAdapter as DualLaunch.DualLaunchInterface)
        val textColor = settingsPreferences.getInt("folder_text", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("folder_text_shadow", Color.BLACK)
        dualLaunch.dualLaunchLabel.setTextColor(textColor)
        dualLaunch.dualLaunchLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.addView(dualLaunch)
        parentLayout.removeView(this)
        parentActivity.persistGrid(page)
    }

    fun setIconSize(pref: String) {
        val percentage = settingsPreferences.getInt(pref, 100)
        val scale = percentage.toFloat() / 100
        this.icon.scaleX = scale
        this.icon.scaleY = scale
    }

    /*
    fun convertToWidget(widgetInfo: WidgetInfo) {
        val widgetContainer = WidgetContainer(
            parentActivity,
            widgetInfo.getAppWidgetId(),
            widgetInfo.getAppWidgetProviderInfo()
        )
        val params = this.layoutParams as HomeLayout.LayoutParams
        widgetContainer.layoutParams = params
        widgetContainer.setListener(parentActivity as WidgetContainer.WidgetInterface)
        parentLayout.addView(widgetContainer)
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.removeView(this)
    }

     */

    interface IconInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onLongClick(view: View)
        fun resetResize()
        fun onUninstall(launchInfo: LaunchInfo)
        fun onRemoveFromFolder(launchInfo: LaunchInfo)
        fun onReloadAppDrawer()
        fun onOpenDrawer()
    }
}