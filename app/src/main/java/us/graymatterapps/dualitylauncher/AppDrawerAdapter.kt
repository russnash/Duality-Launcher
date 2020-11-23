package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import us.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor


class AppDrawerAdapter(
    private val parentActivity: MainActivity,
    private val apps: MutableList<AppList.AppListDataType>
) : RecyclerView.Adapter<AppDrawerAdapter.AppDrawerHolder>(),
    Icon.IconInterface {

    private lateinit var listener: DrawerAdapterInterface
    val settingsPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(parentActivity)
    var filteredList: MutableList<AppList.AppListDataType> = ArrayList()
    var filteredWork: Boolean = false
    val TAG = javaClass.simpleName

    fun filterWork(work: Boolean) {
        val currentUser = android.os.Process.myUserHandle()

        appList.lock.lock()

        if (work) {
            filteredList =
                apps.filter { it.handle != currentUser || appList.isManualWorkApp(LaunchInfo(it.activityName, it.packageName, it.userSerial))} as MutableList<AppList.AppListDataType>
        } else {
            filteredList =
                apps.filter { it.handle.equals(currentUser) && !appList.isManualWorkApp(LaunchInfo(it.activityName, it.packageName, it.userSerial)) } as MutableList<AppList.AppListDataType>
        }

        appList.lock.unlock()

        filteredWork = work
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppDrawerHolder {
        val inflatedView = Icon(
            parentActivity,
            null,
            false,
            9999
        )
        return AppDrawerHolder(
            inflatedView
        )
    }

    class AppDrawerHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${filteredList.size}")
        return filteredList.size
    }

    override fun onBindViewHolder(holder: AppDrawerHolder, position: Int) {
        val iconPadding = settingsPreferences.getInt("drawer_icon_padding", 5)
        val textSize = settingsPreferences.getInt("drawer_text_size", 14)
        val icon = holder.itemView as Icon
        icon.page = position
        icon.label.setTextColor(
            settingsPreferences.getInt(
                "app_drawer_text",
                Color.WHITE
            )
        )
        val textShadowColor = settingsPreferences.getInt("app_drawer_text_shadow", Color.BLACK)
        icon.label.setShadowLayer(6f, 0f, 0f, textShadowColor)
        icon.setLaunchInfo(
            filteredList[position].activityName,
            filteredList[position].packageName,
            filteredList[position].userSerial
        )
        icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        icon.label.textSize = textSize.toFloat()
        icon.setListener(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (parentActivity is DrawerAdapterInterface) {
            listener = parentActivity
        } else {
            throw ClassCastException(parentActivity.toString() + " must implement DrawerAdapterInterface.")
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    interface DrawerAdapterInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onUninstall(launchInfo: LaunchInfo)
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
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
        // Do nothing
    }

    override fun onReloadAppDrawer() {
        filterWork(filteredWork)
        this.notifyDataSetChanged()
    }
}