package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WidgetDB(val con: Context) {
    val widgets: ArrayList<WidgetDBDataType> = ArrayList()
    var sizes: ArrayList<WidgetDBSizes> = ArrayList()
    val TAG = javaClass.simpleName
    private lateinit var listener: WidgetDBInterface

    init {
        var loadItJson = prefs.getString("widgetSizes", "")
        if (loadItJson != "") {
            sizes = loadItJson?.let { Json.decodeFromString(it) }!!
        }
    }

    private fun persistSizes() {
        val saveItJson = Json.encodeToString(sizes)
        val editor = prefs.edit()
        editor.putString("widgetSizes", saveItJson)
        editor.apply()
    }

    fun updateWidgetSize(appWidgetId: Int, rowSpan: Int, columnSpan: Int) {
        var found = false
        for (i in 0 until sizes.size) {
            if (sizes[i].widgetId == appWidgetId) {
                sizes[i].rowSpan = rowSpan
                sizes[i].columnSpan = columnSpan
                found = true
                break
            }
        }
        if (found == false) {
            sizes.add(
                WidgetDBSizes(
                    appWidgetId,
                    rowSpan,
                    columnSpan
                )
            )
        }
        persistSizes()
    }

    fun getWidgetSize(appWidgetId: Int): Bundle {
        var rowSpan = 0
        var columnSpan = 0
        for (i in 0 until sizes.size) {
            if (sizes[i].widgetId == appWidgetId) {
                rowSpan = sizes[i].rowSpan
                columnSpan = sizes[i].columnSpan
                break
            }
        }

        var result = Bundle()
        result.putInt("rowSpan", rowSpan)
        result.putInt("columnSpan", columnSpan)
        return result
    }

    fun allocateWidget(
        appWidgetId: Int,
        appWidgetProviderInfo: AppWidgetProviderInfo,
        requestingContainer: WidgetContainer
    ): Int {
        for (i in 0 until widgets.size) {
            if (widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "allocateWidget() appWidgetId $appWidgetId found at index $i.")
                widgets[i].widgetContainer = requestingContainer
                if(widgets[i].initialized) {
                    Log.d(TAG, "allocateWidget() Widget initialized, performing addWidgetView()")
                    widgets[i].widgetContainer.addWidgetView()
                }
                listener.showWidgets()
                return i
            }
        }

        widgets.add(
            WidgetDBDataType(
                appWidgetId,
                appWidgetProviderInfo,
                AppWidgetHostView(con),
                requestingContainer
            )
        )

        for (i in 0 until widgets.size) {
            if (widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "allocateWidget() appWidgetId $appWidgetId created at index $i.")
                listener.initializeWidget(
                    widgets[i].appWidgetHostView,
                    appWidgetId,
                    appWidgetProviderInfo
                )
                listener.showWidgets()
                return i
            }
        }
        Log.d(TAG, "allocateWidget() Shouldn't have gotten here!")
        return -1
    }

    fun getWidgetIndex(appWidgetId: Int): Int {
        for (i in 0 until widgets.size) {
            if (widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "getWidgetIndex() appWidgetId $appWidgetId found at index $i.")
                listener.showWidgets()
                return i
            }
        }
        Log.d(TAG, "getWidgetIndex() Shouldn't have gotten here!")
        return -1
    }

    fun deleteWidget(appWidgetId: Int) {
        for (i in 0 until widgets.size) {
            if (widgets[i].widgetId == appWidgetId) {
                Log.d(TAG, "deleteWidget() appWidgetId $appWidgetId found at index $i and destroyed.")
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                widgets.removeAt(i)
                listener.showWidgets()
                break
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

    @Serializable
    data class WidgetDBSizes(
        var widgetId: Int,
        var rowSpan: Int,
        var columnSpan: Int
    )

    fun setListener(ear: WidgetDBInterface) {
        listener = ear
    }

    interface WidgetDBInterface {
        fun initializeWidget(
            hostView: AppWidgetHostView,
            widgetId: Int,
            widgetProviderInfo: AppWidgetProviderInfo
        )

        fun showWidgets()
    }
}