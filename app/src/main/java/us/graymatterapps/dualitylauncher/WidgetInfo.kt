package us.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetProviderInfo
import android.view.View

class WidgetInfo(
    private var appWidgetId: Int,
    private var appWidgetProviderInfo: AppWidgetProviderInfo,
    private var previewView: View?
) {
    val TAG = javaClass.simpleName

    fun setAppWidgetId(id: Int) {
        appWidgetId = id
    }

    fun getAppWidgetId(): Int {
        return appWidgetId
    }

    fun setAppWidgetProviderInfo(providerInfo: AppWidgetProviderInfo) {
        appWidgetProviderInfo = providerInfo
    }

    fun getAppWidgetProviderInfo(): AppWidgetProviderInfo {
        return appWidgetProviderInfo
    }

    fun setPreviewView(preview: View) {
        previewView = preview
    }

    fun getPreviewView(): View? {
        return previewView
    }
}