package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TableLayout
import androidx.gridlayout.widget.GridLayout
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData

class WidgetContainer(context: Context, private val appWidgetId: Int, private val appWidgetProviderInfo: AppWidgetProviderInfo): FrameLayout(context), GestureDetector.OnGestureListener{

    lateinit var appWidget: AppWidgetHostView
    lateinit var parentLayout: TableLayout
    lateinit var gestureDetector: GestureDetector
    var cellWidth: Int = 0
    var cellHeight: Int = 0

    override fun onAttachedToWindow() {
        gestureDetector = GestureDetector(context, this)

        parentLayout = this.parent as TableLayout
        cellWidth = parentLayout.width / 8
        cellHeight = parentLayout.height / 8

        appWidget = appWidgetHost.createView(context, appWidgetId, appWidgetProviderInfo)
        val neededWidth = appWidgetProviderInfo.minWidth + appWidget.paddingLeft + appWidget.paddingRight
        val neededHeight = appWidgetProviderInfo.minHeight + appWidget.paddingTop + appWidget.paddingBottom
        val neededColumns = (neededWidth + cellWidth -1) / cellWidth
        val neededRows = (neededHeight + cellHeight -1) / cellHeight
        val params = GridLayout.LayoutParams()
        params.columnSpec = GridLayout.spec(0, neededColumns)
        params.rowSpec = GridLayout.spec(0, neededRows)
        this.layoutParams = params
        appWidget.minimumWidth = neededWidth
        appWidget.minimumHeight = neededHeight
        appWidget.setPadding(0, 0, 0, 0)
        appWidget.updateAppWidgetSize(null, appWidgetProviderInfo.minWidth, appWidgetProviderInfo.minHeight, appWidgetProviderInfo.minWidth, appWidgetProviderInfo.minHeight)
        this.addView(appWidget)

        super.onAttachedToWindow()
    }

    fun convertToIcon(launchInfo: LaunchInfo){
        val icon = Icon(mainContext, null)
        icon.setLaunchInfo(launchInfo)
        icon.setBlankOnDrag(true)
        parentLayout.addView(icon)
        parentLayout.removeView(this)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onInterceptTouchEvent(event)
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(p0: MotionEvent?) {
        // Do nothing
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent?) {
        val id = System.currentTimeMillis().toString()
        dragAndDropData.addWidget(appWidgetId, id)
        val clipData = ClipData.newPlainText("widget", appWidgetId.toString())
        val dsb = DragShadowBuilder(this)
        this.startDrag(clipData, dsb, this, 0)
        parentLayout.removeView(this)
        true
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }
}