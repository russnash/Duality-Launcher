package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import us.graymatterapps.dualitylauncher.appdb.AppDB
import us.graymatterapps.dualitylauncher.components.Icon


class AppDrawerAdapter(
    private val parentActivity: MainActivity
) : RecyclerView.Adapter<AppDrawerAdapter.AppDrawerHolder>(),
    Icon.IconInterface {

    private lateinit var listener: DrawerAdapterInterface
    val settingsPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(parentActivity)
    var filteredWork: Boolean = false
    val db = appList.appDB.readableDatabase
    lateinit var cursor: Cursor
    val TAG = javaClass.simpleName

    fun filterWork(work: Boolean) {
        val currentUser =
            appList.userManager.getSerialNumberForUser(android.os.Process.myUserHandle())

        appList.lock.lock()

        if (work) {
            cursor = db.rawQuery(
                "select activityName, packageName, userSerial from ${AppDB.TABLENAME} where userSerial<>$currentUser or manualWork=1 order by name asc",
                null
            )
        } else {
            cursor = db.rawQuery(
                "select activityName, packageName, userSerial from ${AppDB.TABLENAME} where userSerial=$currentUser and manualWork=0 order by name asc",
                null
            )
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
        return cursor.count
    }

    override fun onBindViewHolder(holder: AppDrawerHolder, position: Int) {
        cursor.moveToPosition(position)
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
            cursor.getString(0),
            cursor.getString(1),
            cursor.getLong(2)
        )
        icon.setIconSize("drawer_icon_size")
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

    override fun onOpenDrawer() {
        // Do nothing
    }
}