package com.graymatterapps.dualitylauncher

class DragAndDropData() {
    var ids: MutableList<String> = ArrayList()
    var launchInfos: MutableList<LaunchInfo> = ArrayList()

    fun add(launchInfo: LaunchInfo, id: String){
        ids.add(id)
        launchInfos.add(launchInfo)
    }

    fun retrieve(id: String) : LaunchInfo {
        val position = ids.indexOf(id)
        val launchInfo = launchInfos[position]
        return launchInfo
    }

    fun reset(){
        ids.clear()
        launchInfos.clear()
    }
}