package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor
import kotlinx.android.synthetic.main.folder.view.*
import kotlinx.android.synthetic.main.icon.view.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.locks.ReentrantLock

class HomePagerAdapter(private val parent: MainActivity, private val container: ViewGroup) :
    RecyclerView.Adapter<HomePagerAdapter.HomePagerHolder>(), Icon.IconInterface,
    Folder.FolderInterface {

    var homeIconsGrid = HomeIconsGrid()
    var homeWidgetsGrid = HomeWidgetsGrid()
    var numRows: Int = 0
    var numCols: Int = 0
    private lateinit var listener: HomeIconsInterface
    var firstRun = arrayOf(true, true, true, true, true)
    val TAG = javaClass.simpleName
    val lock = ReentrantLock()

    class HomePagerHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePagerHolder {
        val inflatedView = LayoutInflater.from(container.context).inflate(R.layout.home_icons, container, false)
        return HomePagerHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: HomePagerHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder() firstRun=${firstRun[position]} displayId=${parent.displayId} position=$position")
        lock.lock()
        val itemView = holder.itemView
        itemView.tag = position
        val homeIconsTable = itemView.findViewById<HomeLayout>(R.id.homeIconsTable)

        if(parent.displayId == 1 && position == 0) {
            Log.d(TAG, "Breakpoint")
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

        if(firstRun[position]) {
            homeIconsTable.removeAllViews()
        } else {
            for (n: Int in 0 until homeIconsTable.childCount){
                if(homeIconsTable.getChildAt(n) is Icon) {
                    homeIconsTable.removeView(homeIconsTable.getChildAt(n))
                }
                if(homeIconsTable.getChildAt(n) is Folder) {
                    homeIconsTable.removeView(homeIconsTable.getChildAt(n))
                }
            }
        }

        homeIconsTable.setGridSize(numRows, numCols)

        for (row in 0 until numRows) {
            for (column in 0 until numCols) {
                val appWidgetId = homeWidgetsGrid.getWidgetId(row, column)
                if (appWidgetId != 0 && firstRun[position]) {
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
                        var icon = Icon(parent, null)
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
                        icon.setListener(this)
                        icon.setBlankOnDrag(true)
                        icon.setDockIcon(false)
                        icon.setLaunchInfo(
                            launchInfo.getActivityName(),
                            launchInfo.getPackageName(),
                            launchInfo.getUserSerial()
                        )
                        homeIconsTable.addView(icon, iconParams)
                    }
                    if (launchInfo.getType() == LaunchInfo.FOLDER) {
                        var folder = Folder(
                            parent,
                            null,
                            parent.getString(R.string.new_folder),
                            launchInfo
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
                        homeIconsTable.addView(folder, folderParams)
                    }
                }
            }
        }
        firstRun[position] = false
        //if(parent.displayId == 1 && position == 0) {
        //    dumpGrid(homeIconsTable, position)
        //}
        lock.unlock()
    }

    fun dumpGrid(homeLayout: HomeLayout, position: Int) {
        for(n in 0 until homeLayout.childCount) {
            if(homeLayout.getChildAt(n) is Icon) {
                val icon = homeLayout.getChildAt(n) as Icon
                Log.d("dumpGrid", "(${parent.displayId}-$position) Icon: ${icon.getLaunchInfo().getActivityName()}")
            }
            if(homeLayout.getChildAt(n) is Folder) {
                val folder = homeLayout.getChildAt(n) as Folder
                Log.d("dumpGrid", "(${parent.displayId}-$position) Folder: ${folder.getLaunchInfo().getFolderName()}")
            }
            if(homeLayout.getChildAt(n) is WidgetContainer) {
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

    override fun onIconChanged() {
        listener.onIconChanged()
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        listener.onDragStarted(view, clipData)
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

    fun setListener(homeIconsInterface: HomeIconsInterface) {
        listener = homeIconsInterface
    }

    interface HomeIconsInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onIconChanged()
        fun onLongClick(view: View)
        fun onShowFolder(state: Boolean)
        fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder)
        fun onFolderChanged()
        fun resetResize()
    }

    override fun onShowFolder(state: Boolean) {
        listener.onShowFolder(state)
    }

    override fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder) {
        listener.onSetupFolder(apps, name, folder)
    }

    override fun onFolderChanged() {
        listener.onFolderChanged()
    }
}