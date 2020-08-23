package com.graymatterapps.dualitylauncher

import kotlinx.serialization.Serializable

@Serializable
data class DockItems(var activityNames: ArrayList<String> = ArrayList(),
                     var packageNames: ArrayList<String> = ArrayList(),
                     var userSerials: ArrayList<Long> = ArrayList()){
    init{
        initialize()
    }

    private fun initialize(){
        MainActivity.appList.waitForReady()
        MainActivity.appList.lock.lock()
        for(n in 0..7){
            add(MainActivity.appList.apps[n].activityName, MainActivity.appList.apps[n].packageName, MainActivity.appList.apps[n].userSerial)
        }
        MainActivity.appList.lock.unlock()
    }

    fun add(activityName: String, packageName: String, userSerial: Long){
        activityNames.add(activityName)
        packageNames.add(packageName)
        userSerials.add(userSerial)
    }
}