package com.graymatterapps.dualitylauncher

class DragAndDropData() {
    var ids: MutableList<String> = ArrayList()
    var launchInfos: MutableList<LaunchInfo> = ArrayList()
    var widgets: MutableList<WidgetInfo> = ArrayList()

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

    fun addWidget(widgetInfo: WidgetInfo, id: String) {
        reset()
        ids.add(id)
        widgets.add(widgetInfo)
    }

    fun retrieveWidgetId(id: String) : WidgetInfo {
        val position = ids.indexOf(id)
        val widgetId = widgets[position]
        reset()
        return widgetId
    }

    fun reset(){
        ids.clear()
        launchInfos.clear()
        widgets.clear()
    }
}