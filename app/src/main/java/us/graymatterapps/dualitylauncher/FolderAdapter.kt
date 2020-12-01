package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.text.Editable
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.dual_launch.view.*

class FolderAdapter(var parentActivity: MainActivity, var apps: ArrayList<LaunchInfo>, val folder: Folder) : BaseAdapter(), Icon.IconInterface, DualLaunch.DualLaunchInterface, SharedPreferences.OnSharedPreferenceChangeListener{
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
        val textColor = settingsPreferences.getInt("folder_text", Color.WHITE)
        val textShadowColor = settingsPreferences.getInt("folder_text_shadow", Color.BLACK)
        val info = apps[position]
        if(info.getType() == LaunchInfo.ICON) {
            val icon = Icon(parentActivity, null, "", "", 0L, false, false, false, 0)
            icon.setLaunchInfo(info)
            icon.setDockIcon(true)
            icon.setListener(this)
            icon.setDragTarget(false)
            icon.label.setTextColor(textColor)
            icon.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
            icon.label.textSize = textSize.toFloat()
            icon.setPadding(iconPadding, iconPadding, iconPadding, 25)
            icon.setOnDragListener { view, dragEvent ->
                if(dragEvent != null) {
                    when (dragEvent.action) {
                        DragEvent.ACTION_DROP -> {
                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                            val launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                            folder.addFolderItemAtPosition(launchInfo, info)
                        }
                    }
                }
                true
            }
            return icon
        }
        if(info.getType() == LaunchInfo.DUALLAUNCH) {
            val dualLaunch = DualLaunch(parentActivity, null, info.getDualLaunchName(), info, false, 0)
            dualLaunch.dualLaunchLabel.setTextColor(textColor)
            dualLaunch.dualLaunchLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
            dualLaunch.dualLaunchLabel.textSize = textSize.toFloat()
            dualLaunch.setPadding(iconPadding, iconPadding, iconPadding, 25)
            dualLaunch.setListener(this)
            dualLaunch.setOnDragListener { view, dragEvent ->
                if(dragEvent != null) {
                    when (dragEvent.action) {
                        DragEvent.ACTION_DROP -> {
                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                            val launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                            folder.addFolderItemAtPosition(launchInfo, info)
                        }
                    }
                }
                true
            }
            return dualLaunch
        }
        return View(parentActivity, null)
    }

    override fun onShowDualLaunch(state: Boolean) {
        parentActivity.onShowDualLaunch(state)
    }

    override fun onSetupDualLaunch(
        apps: ArrayList<LaunchInfo>,
        name: Editable,
        dualLaunch: DualLaunch
    ) {
        parentActivity.onSetupDualLaunch(apps, name, dualLaunch)
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        if(view is Icon) {
            val icon = view as Icon
            folder.removeFolderApp(icon.getLaunchInfo())
        }
        if(view is DualLaunch) {
            val dualLaunch = view as DualLaunch
            folder.removeFolderApp(dualLaunch.getLaunchInfo())
        }
        listener.onDragStarted(view, clipData)
    }

    override fun onDualLaunchUpdated() {
        folder.sortFolder()
        folder.persistFolderApps()
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