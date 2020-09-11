package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.Log

class WidgetDB(val con: Context) {
    val widgets: ArrayList<WidgetDBDataType> = ArrayList()
    val TAG = javaClass.simpleName

    fun getWidget(appWidgetId: Int) : Int? {
        for(i in 0 until widgets.size) {
            if(widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "appWidgetId $appWidgetId found at index $i.")
                return i
            }
        }

        widgets.add(WidgetDBDataType(
            appWidgetId,
            AppWidgetHostView(con)
        ))

        for(i in 0 until widgets.size) {
            if(widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "appWidgetId $appWidgetId created at index $i.")
                return i
            }
        }

        return null
    }

    data class WidgetDBDataType(
        var widgetId: Int,
        var appWidgetHostView: AppWidgetHostView,
        var initialized: Boolean = false
    )
}