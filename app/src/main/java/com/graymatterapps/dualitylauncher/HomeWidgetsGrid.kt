package com.graymatterapps.dualitylauncher

import kotlinx.serialization.Serializable

@Serializable
class HomeWidgetsGrid {
    var widgitIds = arrayOf(
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )
    val TAG = javaClass.simpleName

    fun changeWidgetId(row: Int, column: Int, widgetId: Int) {
        widgitIds[row][column] = widgetId
    }

    fun getWidgetId(row: Int, column: Int): Int {
        return widgitIds[row][column]
    }
}