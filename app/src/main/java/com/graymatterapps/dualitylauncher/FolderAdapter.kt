package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

class FolderAdapter(var context: Context, var apps: ArrayList<LaunchInfo>, val folder: Folder) : BaseAdapter(), Icon.IconInterface, SharedPreferences.OnSharedPreferenceChangeListener{
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
        val icon = Icon(context, null)
        icon.setLaunchInfo(apps[position])
        icon.setDockIcon(true)
        icon.setListener(this)
        return icon
    }

    override fun onIconChanged() {
        // Do nothing
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

    fun setListener(ear: FolderAdapterInterface) {
        listener = ear
    }

    interface FolderAdapterInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        if(key != null) {
            if(key.contains("folder")){
                this.notifyDataSetChanged()
            }
        }
    }
}