package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log

class WidgetDB(val con: Context) {
    val widgets: ArrayList<WidgetDBDataType> = ArrayList()
    val TAG = javaClass.simpleName
    private lateinit var listener: WidgetDBInterface

    fun allocateWidget(appWidgetId: Int, appWidgetProviderInfo: AppWidgetProviderInfo, requestingContainer: WidgetContainer) : Int {
        for(i in 0 until widgets.size) {
            if(widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "(container) appWidgetId $appWidgetId found at index $i.")
                widgets[i].widgetContainer = requestingContainer
                listener.showWidgets()
                return i
            }
        }

        widgets.add(WidgetDBDataType(
            appWidgetId,
            appWidgetProviderInfo,
            AppWidgetHostView(con),
            requestingContainer
        ))

        for(i in 0 until widgets.size) {
            if(widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "(container) appWidgetId $appWidgetId created at index $i.")
                listener.initializeWidget(widgets[i].appWidgetHostView, appWidgetId, appWidgetProviderInfo)
                listener.showWidgets()
                return i
            }
        }
        Log.d(TAG, "(container) Shouldn't have gotten here!")
        return -1
    }

    fun getWidgetIndex(appWidgetId: Int) : Int {
        for(i in 0 until widgets.size) {
            if(widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "(activity) appWidgetId $appWidgetId found at index $i.")
                listener.showWidgets()
                return i
            }
        }
        Log.d(TAG, "(activity) Shouldn't have gotten here!")
        return -1
    }

    fun deleteWidget(appWidgetId: Int) {
        for(i in 0 until widgets.size) {
            if(widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "(delete) appWidgetId $appWidgetId found at index $i and destroyed.")
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                widgets.removeAt(i)
                listener.showWidgets()
            }
        }
    }

    data class WidgetDBDataType(
        var widgetId: Int,
        var widgetProviderInfo: AppWidgetProviderInfo,
        var appWidgetHostView: AppWidgetHostView,
        var widgetContainer: WidgetContainer,
        var initialized: Boolean = false
    )

    fun setListener(ear: WidgetDBInterface) {
        listener = ear
    }

    interface WidgetDBInterface {
        fun initializeWidget(hostView: AppWidgetHostView, widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo)
        fun showWidgets()
    }
}