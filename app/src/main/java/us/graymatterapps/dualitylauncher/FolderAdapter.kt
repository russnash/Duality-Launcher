package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

class FolderAdapter(var parentActivity: MainActivity, var apps: ArrayList<LaunchInfo>, val folder: Folder) : BaseAdapter(), Icon.IconInterface, SharedPreferences.OnSharedPreferenceChangeListener{
    private lateinit var listener: FolderAdapterInterface

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun getCount(): Int {
        return apps.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View {
        val iconPadding = settingsPreferences.getInt("folder_icon_padding", 5)
        val textSize = settingsPreferences.getInt("folder_text_size", 14)
        val icon = Icon(parentActivity, null, false, 0)
        icon.setLaunchInfo(apps[position])
        icon.setDockIcon(true)
        icon.setListener(this)
        val textColor = settingsPreferences.getInt("folder_text", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("folder_text_shadow", Color.BLACK)
        icon.label.setTextColor(textColor)
        icon.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
        icon.label.textSize = textSize.toFloat()
        icon.setPadding(iconPadding, iconPadding, iconPadding, 25)
        return icon
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        val icon = view as Icon
        folder.removeFolderApp(icon.getLaunchInfo())
        listener.onDragStarted(view, clipData)
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        listener.onLaunch(launchInfo, displayId)
    }

    override fun onLongClick(view: View) {
        // Do nothing
    }

    override fun resetResize() {
        // Do nothing
    }

    override fun onUninstall(launchInfo: LaunchInfo) {
        listener.onUninstall(launchInfo)
    }

    override fun onRemoveFromFolder(launchInfo: LaunchInfo) {
        folder.removeFolderApp(launchInfo)
    }

    override fun onReloadAppDrawer() {
        // Do nothing
    }

    fun setListener(ear: FolderAdapterInterface) {
        listener = ear
    }

    interface FolderAdapterInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onUninstall(launchInfo: LaunchInfo)
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        if(key != null) {
            if(key.contains("folder")){
                this.notifyDataSetChanged()
            }
        }
    }
}