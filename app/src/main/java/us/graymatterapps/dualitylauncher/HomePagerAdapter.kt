package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.graphics.Color
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dual_launch.view.*
import kotlinx.android.synthetic.main.folder.view.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.locks.ReentrantLock

class HomePagerAdapter(private val parent: MainActivity, private val container: ViewGroup) :
    RecyclerView.Adapter<HomePagerAdapter.HomePagerHolder>(), Icon.IconInterface,
    Folder.FolderInterface, DualLaunch.DualLaunchInterface {

    var homeIconsGrid = HomeIconsGrid()
    var homeWidgetsGrid = HomeWidgetsGrid()
    var numRows: Int = 0
    var numCols: Int = 0
    private lateinit var listener: HomeIconsInterface
    val TAG = javaClass.simpleName
    val lock = ReentrantLock()

    class HomePagerHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePagerHolder {
        val inflatedView =
            LayoutInflater.from(container.context).inflate(R.layout.home_icons, container, false)
        return HomePagerHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: HomePagerHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder() displayId=${parent.displayId} position=$position")
        lock.lock()
        val itemView = holder.itemView
        itemView.tag = position
        val homeIconsTable = itemView.findViewById<HomeLayout>(R.id.homeIconsTable)
        homeIconsTable.parentActivity = parent
        homeIconsTable.page = position

        if (parent.displayId == 1 && position == 0) {
            Log.d(TAG, "Breakpoint")
        }

        itemView.setOnClickListener {
            listener.resetResize()
        }
        itemView.setOnLongClickListener { view ->
            listener.onLongClick(view)
            true
        }
        homeIconsGrid = HomeIconsGrid()
        homeWidgetsGrid = HomeWidgetsGrid()
        depersistGrid(position)

        val numColsString = settingsPreferences.getString("home_grid_columns", "6")
        numCols = Integer.parseInt(numColsString.toString())
        val numRowsString = settingsPreferences.getString("home_grid_rows", "7")
        numRows = Integer.parseInt(numRowsString.toString())
        val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
        val iconPadding = settingsPreferences.getInt("home_icon_padding", 5)
        val textSize = settingsPreferences.getInt("home_text_size", 14)

        homeIconsTable.removeAllViews()
        homeIconsTable.setGridSize(numRows, numCols)

        for (row in 0 until numRows) {
            for (column in 0 until numCols) {
                val appWidgetId = homeWidgetsGrid.getWidgetId(row, column)
                if (appWidgetId != 0) {
                    var widgetParams = HomeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    widgetParams.row = row
                    widgetParams.column = column
                    widgetParams.rowSpan = 1
                    widgetParams.columnSpan = 1
                    val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                    if (appWidgetProviderInfo != null) {
                        var widget = WidgetContainer(
                            parent,
                            homeWidgetsGrid.getWidgetId(row, column),
                            appWidgetProviderInfo
                        )
                        widget.setListener(parent)
                        homeIconsTable.addView(widget, widgetParams)
                    }
                } else {
                    val launchInfo = homeIconsGrid.getLaunchInfo(row, column)
                    if (launchInfo.getType() == LaunchInfo.ICON) {
                        if (launchInfo.getActivityName() != "") {
                            var icon = Icon(parent, null, false, position)
                            var iconParams = HomeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            iconParams.row = row
                            iconParams.column = column
                            iconParams.rowSpan = 1
                            iconParams.columnSpan = 1
                            icon.layoutParams = iconParams
                            icon.label.setTextColor(textColor)
                            icon.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
                            icon.label.textSize = textSize.toFloat()
                            icon.setListener(this)
                            icon.setBlankOnDrag(false)
                            icon.setDockIcon(false)
                            icon.setLaunchInfo(
                                launchInfo.getActivityName(),
                                launchInfo.getPackageName(),
                                launchInfo.getUserSerial()
                            )
                            icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                            homeIconsTable.addView(icon, iconParams)
                        }
                    }
                    if (launchInfo.getType() == LaunchInfo.FOLDER) {
                        var folder = Folder(
                            parent,
                            null,
                            launchInfo.getFolderName(),
                            launchInfo,
                            false,
                            position
                        )
                        folder.setListener(this)
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
                        folder.folderLabel.textSize = textSize.toFloat()
                        folder.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                        homeIconsTable.addView(folder, folderParams)
                    }
                    if (launchInfo.getType() == LaunchInfo.DUALLAUNCH) {
                        val dualLaunch = DualLaunch(
                            parent,
                            null,
                            launchInfo.getDualLaunchName(),
                            launchInfo,
                            false,
                            position
                        )
                        dualLaunch.setListener(this)
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
                        dualLaunch.dualLaunchLabel.textSize = textSize.toFloat()
                        dualLaunch.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                        homeIconsTable.addView(dualLaunch, dualLaunchParams)
                    }
                }
            }
        }
        //if(parent.displayId == 1 && position == 0) {
        //    dumpGrid(homeIconsTable, position)
        //}
        lock.unlock()
    }

    fun dumpGrid(homeLayout: HomeLayout, position: Int) {
        for (n in 0 until homeLayout.childCount) {
            if (homeLayout.getChildAt(n) is Icon) {
                val icon = homeLayout.getChildAt(n) as Icon
                Log.d(
                    "dumpGrid",
                    "(${parent.displayId}-$position) Icon: ${
                        icon.getLaunchInfo().getActivityName()
                    }"
                )
            }
            if (homeLayout.getChildAt(n) is Folder) {
                val folder = homeLayout.getChildAt(n) as Folder
                Log.d(
                    "dumpGrid",
                    "(${parent.displayId}-$position) Folder: ${
                        folder.getLaunchInfo().getFolderName()
                    }"
                )
            }
            if (homeLayout.getChildAt(n) is WidgetContainer) {
                val widget = homeLayout.getChildAt(n) as WidgetContainer
                Log.d("dumpGrid", "(${parent.displayId}-$position) Widget: ${widget.appWidgetId}")
            }
        }
    }

    override fun getItemCount(): Int {
        val homePagesString = settingsPreferences.getString("home_grid_pages", "1")
        return Integer.parseInt(homePagesString.toString())
    }

    fun depersistGrid(position: Int) {
        Log.d(TAG, "depersistGrid()")
        var loadItJson = prefs.getString("homeIconsGrid" + position, "")
        if (loadItJson != "") {
            homeIconsGrid = loadItJson?.let { Json.decodeFromString(it) }!!
        }
        loadItJson = prefs.getString("homeWidgetsGrid" + parent.displayId + ":" + position, "")
        if (loadItJson != "") {
            homeWidgetsGrid = loadItJson?.let { Json.decodeFromString(it) }!!
        }
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        listener.onDragStarted(view, clipData)
    }

    override fun onDualLaunchUpdated() {
        // Do nothing
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        listener.onLaunch(launchInfo, displayId)
    }

    override fun onLongClick(view: View) {
        listener.onLongClick(view)
    }

    override fun resetResize() {
        listener.resetResize()
    }

    override fun onUninstall(launchInfo: LaunchInfo) {
        listener.onUninstall(launchInfo)
    }

    override fun onRemoveFromFolder(launchInfo: LaunchInfo) {
        // Do nothing
    }

    override fun onReloadAppDrawer() {
        // Do nothing
    }

    fun setListener(homeIconsInterface: HomeIconsInterface) {
        listener = homeIconsInterface
    }

    interface HomeIconsInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onIconChanged()
        fun onLongClick(view: View)
        fun onShowFolder(state: Boolean)
        fun onShowDualLaunch(state: Boolean)
        fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder)
        fun onSetupDualLaunch(apps: ArrayList<LaunchInfo>, name: Editable, dualLaunch: DualLaunch)
        fun onFolderChanged()
        fun resetResize()
        fun onUninstall(launchInfo: LaunchInfo)
    }

    override fun onShowFolder(state: Boolean) {
        listener.onShowFolder(state)
    }

    override fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder) {
        listener.onSetupFolder(apps, name, folder)
    }

    override fun onShowDualLaunch(state: Boolean) {
        listener.onShowDualLaunch(state)
    }

    override fun onSetupDualLaunch(
        apps: ArrayList<LaunchInfo>,
        name: Editable,
        dualLaunch: DualLaunch
    ) {
        listener.onSetupDualLaunch(apps, name, dualLaunch)
    }
}