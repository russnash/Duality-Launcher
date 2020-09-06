package com.graymatterapps.dualitylauncher

import kotlinx.serialization.Serializable

@Serializable
data class DockItems(var activityNames: ArrayList<String> = ArrayList(),
                     var packageNames: ArrayList<String> = ArrayList(),
                     var userSerials: ArrayList<Long> = ArrayList()){
    val TAG = javaClass.simpleName
    init{
        initialize()
    }

    private fun initialize(){
        appList.waitForReady()
        appList.lock.lock()
        for(n in 0..7){
            add(appList.apps[n].activityName, appList.apps[n].packageName, appList.apps[n].userSerial)
        }
        appList.lock.unlock()
    }

    fun add(activityName: String, packageName: String, userSerial: Long){
        activityNames.add(activityName)
        packageNames.add(packageName)
        userSerials.add(userSerial)
    }
}