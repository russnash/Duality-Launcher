package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import com.graymatterapps.dualitylauncher.MainActivity.Companion.appList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Dock(context: Context, attrs: AttributeSet): LinearLayout(context, attrs),
    SharedPreferences.OnSharedPreferenceChangeListener, Icon.IconInterface {
    val settingsPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    lateinit var listener: DockInterface
    var prefs: SharedPreferences
    var dockItems = DockItems()
    var dockTable: TableLayout
    var dockRow: TableRow

    init {
        inflate(context, R.layout.dock, this)
        dockTable = findViewById(R.id.dockTable)
        dockRow = findViewById(R.id.dockRow)
        prefs = context.getSharedPreferences(PREFS_FILENAME, 0)
        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
        dockRow.setBackgroundColor(Color.TRANSPARENT)
        setDockBackground()
    }

    fun populateDock(){
        val totalItemsString = settingsPreferences.getString("dock_icons", "6")
        val totalItems = Integer.parseInt(totalItemsString)
        dockRow.removeAllViews()

        if (totalItems != null) {
            for(n in 0..(totalItems -1)){
                var dockIcon = Icon(context, null, dockItems.activityNames[n], dockItems.packageNames[n], dockItems.userSerials[n])
                var params = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
                params.width = 0
                dockIcon.layoutParams = params
                dockIcon.label.height = 0
                dockIcon.setListener(this)
                dockIcon.setBlankOnDrag(true)
                dockRow.addView(dockIcon)
            }
        }
    }

    fun persistDock(){
        dockItems.activityNames.clear()
        dockItems.packageNames.clear()
        dockItems.userSerials.clear()
        for(n in 0 until dockRow.childCount){
            val dockIcon: Icon = dockRow.getChildAt(n) as Icon
            dockItems.add(dockIcon.launchInfo.getActivityName(), dockIcon.launchInfo.getPackageName(), dockIcon.launchInfo.getUserSerial())
        }
        val saveItJson = Json.encodeToString(dockItems)
        val editor = prefs.edit()
        editor.putString("dockItems", saveItJson)
        editor.apply()
    }

    fun depersistDock(){
        while(!appList.ready) {
            Thread.sleep(100)
        }
        val loadItJson = prefs.getString("dockItems","")
        if(loadItJson != ""){
            dockItems = loadItJson?.let { Json.decodeFromString(it) }!!
        }
    }

    fun setDockBackground(){
        if(!settingsPreferences.getBoolean("dock_background", false)) {
            dockTable.setBackgroundColor(Color.TRANSPARENT)
        } else {
            var basicColor = MainActivity.colorPrefToColor(this.settingsPreferences.getString("dock_background_color", "Light Gray"))
            var alpha = this.settingsPreferences.getInt("dock_background_alpha", 80)
            var color = ColorUtils.setAlphaComponent(basicColor, alpha)
            dockTable.setBackgroundColor(color)
        }
    }

    override fun onSharedPreferenceChanged(sharedPrefences: SharedPreferences?, key: String?) {
        if(key == "dockItems"){
            depersistDock()
            populateDock()
        }

        if(key == "dock_icons"){
            depersistDock()
            populateDock()
        }

        if(key == "dock_background"){
            setDockBackground()
        }

        if(key == "dock_background_color"){
            setDockBackground()
        }

        if(key == "dock_background_alpha"){
            setDockBackground()
        }
    }

    fun setListener(mainActivity: MainActivity) {
        listener = mainActivity
    }

    interface DockInterface{
        fun onDragStarted(view: View, clipData: ClipData)
    }

    override fun onIconChanged() {
        persistDock()
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        listener.onDragStarted(view, clipData)
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        appList.launchPackage(launchInfo, displayId)
    }

    fun startDrag(view: View, clipData: ClipData) {
        val dsb = View.DragShadowBuilder(view)
        view.startDrag(clipData, dsb, view, 0)
    }
}