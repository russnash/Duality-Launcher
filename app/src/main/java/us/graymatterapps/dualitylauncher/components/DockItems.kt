package us.graymatterapps.dualitylauncher.components

import kotlinx.serialization.Serializable
import us.graymatterapps.dualitylauncher.appList

@Serializable
data class DockItems(
    var activityNames: ArrayList<String> = ArrayList(),
    var packageNames: ArrayList<String> = ArrayList(),
    var userSerials: ArrayList<Long> = ArrayList()
) {
    val TAG = javaClass.simpleName

    init {
        initialize()
    }

    private fun initialize() {
        appList.waitForReady()
        appList.lock.lock()
        val apps = appList.appDB.get8Apps()
        for (n in 0..7) {
            add(
                apps[n].activityName,
                apps[n].packageName,
                apps[n].userSerial
            )
        }
        appList.lock.unlock()
    }

    fun add(activityName: String, packageName: String, userSerial: Long) {
        activityNames.add(activityName)
        packageNames.add(packageName)
        userSerials.add(userSerial)
    }
}