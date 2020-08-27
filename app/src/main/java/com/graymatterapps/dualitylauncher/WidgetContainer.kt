package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData

class WidgetContainer(
    context: Context,
    var appWidgetId: Int,
    private var appWidgetProviderInfo: AppWidgetProviderInfo,
    val bind: Boolean = false
) : FrameLayout(context), GestureDetector.OnGestureListener {

    lateinit var appWidgetHostView: AppWidgetHostView
    lateinit var gestureDetector: GestureDetector
    lateinit var parentLayout: HomeLayout
    var neededWidth: Int = 0
    var neededHeight: Int = 0
    var isWaitingForPermission: Boolean = false
    lateinit var listener: WidgetInterface

    init {
        if(bind){
            if(appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetProviderInfo.provider)){
                bindWidget()
            } else {
                listener = mainContext as WidgetInterface
                isWaitingForPermission = true
                listener.needPermissionToBind(appWidgetId, appWidgetProviderInfo)
            }
        }
        appWidgetHostView = appWidgetHost.createView(appContext, appWidgetId, appWidgetProviderInfo)
        neededWidth =
            appWidgetProviderInfo.minWidth + appWidgetHostView.paddingLeft + appWidgetHostView.paddingRight
        neededHeight =
            appWidgetProviderInfo.minHeight + appWidgetHostView.paddingTop + appWidgetHostView.paddingBottom
        appWidgetHostView.minimumWidth = neededWidth
        appWidgetHostView.minimumHeight = neededHeight
        appWidgetHostView.setPadding(0, 0, 0, 0)
        appWidgetHostView.updateAppWidgetSize(
            null,
            appWidgetProviderInfo.minWidth,
            appWidgetProviderInfo.minHeight,
            appWidgetProviderInfo.minWidth,
            appWidgetProviderInfo.minHeight
        )
        this.addView(appWidgetHostView)
    }

    override fun onAttachedToWindow() {
        parentLayout = this.parent as HomeLayout
        gestureDetector = GestureDetector(context, this)
        super.onAttachedToWindow()
    }

    fun bindWidget(data: Intent? = null){
        isWaitingForPermission = false
        if(data != null){
            val extras = data.extras
            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            }
        }
    }

    fun convertToIcon() {
        val icon = Icon(mainContext, null)
        val launchInfo = LaunchInfo()
        icon.setLaunchInfo(launchInfo)
        icon.setBlankOnDrag(true)
        val params = this.layoutParams as HomeLayout.LayoutParams
        icon.layoutParams = params
        parentLayout.addView(icon, params)
        parentLayout.removeView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(neededWidth, neededHeight)
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
        val widgetInfo = WidgetInfo(appWidgetId, appWidgetProviderInfo, null)
        dragAndDropData.addWidget(widgetInfo, id)
        val clipData = ClipData.newPlainText("widget", id)
        val dsb = DragShadowBuilder(this)
        this.startDragAndDrop(clipData, dsb, this, 0)
        this.convertToIcon()
        true
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }

    interface WidgetInterface {
        fun needPermissionToBind(widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo)
    }
}