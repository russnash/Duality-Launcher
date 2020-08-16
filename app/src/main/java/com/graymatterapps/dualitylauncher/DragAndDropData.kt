package com.graymatterapps.dualitylauncher

class DragAndDropData() {
    var ids: MutableList<String> = ArrayList()
    var launchInfos: MutableList<LaunchInfo> = ArrayList()
    var widgetIds: MutableList<Int> = ArrayList()

    fun addLaunchInfo(launchInfo: LaunchInfo, id: String){
        reset()
        ids.add(id)
        launchInfos.add(launchInfo)
    }

    fun retrieveLaunchInfo(id: String) : LaunchInfo {
        val position = ids.indexOf(id)
        val launchInfo = launchInfos[position]
        reset()
        return launchInfo
    }

    fun addWidget(widgetId: Int, id: String) {
        reset()
        ids.add(id)
        widgetIds.add(widgetId)
    }

    fun retrieveWidgetId(id: String) : Int {
        val position = ids.indexOf(id)
        val widgetId = widgetIds[position]
        reset()
        return widgetId
    }

    fun reset(){
        ids.clear()
        launchInfos.clear()
        widgetIds.clear()
    }
}