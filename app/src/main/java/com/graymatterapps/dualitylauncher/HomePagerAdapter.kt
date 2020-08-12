package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HomePagerAdapter (private val context: Context): RecyclerView.Adapter<HomePagerAdapter.HomePagerHolder>(), Icon.IconInterface, SharedPreferences.OnSharedPreferenceChangeListener {

    var homeIconsGrid = HomeIconsGrid()
    var numRows: Int = 0
    var numCols: Int = 0
    private lateinit var listener: HomeIconsInterface

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    class HomePagerHolder(view: View): RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePagerHolder {
        val inflatedView = parent.inflate(R.layout.home_icons, false)
        return HomePagerHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: HomePagerHolder, position: Int) {
        val itemView = holder.itemView
        itemView.tag = position
        val homeIconsTable = itemView.findViewById<TableLayout>(R.id.homeIconsTable)

        homeIconsGrid = HomeIconsGrid()
        depersistGrid(position)

        val numColsString = settingsPreferences.getString("home_grid_columns", "6")
        numCols = Integer.parseInt(numColsString)
        val numRowsString = settingsPreferences.getString("home_grid_rows", "7")
        numRows = Integer.parseInt(numRowsString)

        homeIconsTable.removeAllViews()

        var rowParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT)
        rowParams.weight = 1.0f
        rowParams.gravity = Gravity.CENTER
        var iconParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
        iconParams.width = 0

        for(y in 1..numRows){
            var tableRow = TableRow(context)
            tableRow.layoutParams = rowParams

            for(x in 1..numCols){
                var icon = Icon(context, null)
                if (icon != null) {
                    icon.layoutParams = iconParams
                    icon.label.maxLines = 1
                    icon.setListener(this)
                    icon.setBlankOnDrag(true)
                    val launchInfo = homeIconsGrid.get(x-1, y-1)
                    icon.setLaunchInfo(launchInfo.getActivityName(), launchInfo.getPackageName(), launchInfo.getUserSerial())
                }
                tableRow.addView(icon)
            }
            homeIconsTable.addView(tableRow)
        }
    }

    override fun getItemCount(): Int {
        val homePagesString = settingsPreferences.getString("home_grid_pages", "1")
        return Integer.parseInt(homePagesString)
    }

    fun depersistGrid(position: Int){
        val loadItJson = prefs.getString("homeIconsGrid" + position, "")
        if(loadItJson != ""){
            homeIconsGrid = loadItJson?.let { Json.decodeFromString(it) }!!
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

    fun setListener(homeIconsInterface: HomeIconsInterface){
        listener = homeIconsInterface
    }

    interface HomeIconsInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onIconChanged()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        // Do nothing
    }
}