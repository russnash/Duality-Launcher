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

    fun change(row: Int, column: Int, widgetId: Int) {
        widgitIds[row][column] = widgetId
    }

    fun get(row: Int, column: Int): Int {
        return widgitIds[row][column]
    }
}