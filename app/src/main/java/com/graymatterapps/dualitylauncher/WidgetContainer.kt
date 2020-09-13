package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData

class WidgetContainer(
    val parentActivity: MainActivity,
    var appWidgetId: Int,
    private var appWidgetProviderInfo: AppWidgetProviderInfo
) : FrameLayout(parentActivity), GestureDetector.OnGestureListener {

    var widgetDBIndex: Int? = null
    lateinit var parentLayout: HomeLayout
    lateinit var appWidgetHostView: AppWidgetHostView
    var gestureDetector: GestureDetector
    var neededWidth: Int = 0
    var neededHeight: Int = 0
    var isWaitingForPermission: Boolean = false
    var isWaitingForConfigure: Boolean = false
    private lateinit var listener: WidgetInterface
    val TAG = javaClass.simpleName

    init {
        gestureDetector = GestureDetector(parentActivity, this)
    }

    override fun onAttachedToWindow() {
        parentLayout = this.parent as HomeLayout

        if (checkBinding()) {
            buildWidget()
        } else {
            val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(
                appWidgetId,
                appWidgetProviderInfo.provider
            )
            if (canBind) {
                configureWidget()
            } else {
                isWaitingForPermission = true
                listener.needPermissionToBind(appWidgetId, appWidgetProviderInfo)
            }
        }

        super.onAttachedToWindow()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(ev)
    }

    fun setListener(ear: WidgetInterface) {
        listener = ear
    }

    private fun buildWidget() {
        appWidgetHostView =
            appWidgetHost.createView(appContext, appWidgetId, appWidgetProviderInfo)
        appWidgetHostView.setAppWidget(
            appWidgetId,
            appWidgetProviderInfo
        )
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        appWidgetHostView.updateAppWidgetOptions(options)
        layoutWidget()
    }

    private fun longClick() {
        if (!isPaging) {
            val id = System.currentTimeMillis().toString()
            var widgetInfo = WidgetInfo(appWidgetId, appWidgetProviderInfo, null)
            dragAndDropData.addWidget(widgetInfo, id)
            val clipData = ClipData.newPlainText("widget", id)
            val dsb = WidgetDragShadowBuilder(this)
            if (!this.startDragAndDrop(clipData, dsb, this, 0)) {
                widgetInfo = dragAndDropData.retrieveWidgetId(id)
                appWidgetHost.deleteAppWidgetId(widgetInfo.getAppWidgetId())
            }
            this.convertToIcon()
        }
    }

    fun configureWidget(data: Intent? = null) {
        isWaitingForPermission = false

        appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetProviderInfo.configure != null) {
            isWaitingForConfigure = true
            listener.configureWidget(appWidgetId, appWidgetProviderInfo)
        } else {
            createWidget(data)
        }
    }

    fun createWidget(data: Intent? = null) {
        isWaitingForConfigure = false

        val extras = data?.extras
        if (extras != null) {
            val intentWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if(intentWidgetId != -1) {
                appWidgetId = intentWidgetId
            }
        }
        appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        buildWidget()
    }

    private fun layoutWidget() {
        val minWidth =
            appWidgetProviderInfo.minWidth + appWidgetHostView.paddingLeft + appWidgetHostView.paddingRight
        val minHeight =
            appWidgetProviderInfo.minHeight + appWidgetHostView.paddingTop + appWidgetHostView.paddingBottom

        neededWidth = parentLayout.widthToCells(minWidth) * parentLayout.getCellWidth()
        neededHeight = parentLayout.heightToCells(minHeight) * parentLayout.getCellHeight()
        appWidgetHostView.setPadding(0, 0, 0, 0)
        val viewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        viewParams.width = neededWidth
        viewParams.height = neededHeight
        viewParams.gravity = Gravity.CENTER
        appWidgetHostView.layoutParams = viewParams
        val widthSpec = MeasureSpec.makeMeasureSpec(neededWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(neededHeight, MeasureSpec.EXACTLY)
        this.removeAllViews()
        this.addView(appWidgetHostView, viewParams)
        appWidgetHostView.updateAppWidgetSize(
            null,
            neededWidth,
            neededHeight,
            neededWidth,
            neededHeight
        )
        appWidgetHostView.measure(widthSpec, heightSpec)
        this.bringToFront()
        if(appWidgetProviderInfo.provider.packageName.contains("jet")){
            Log.d(TAG, "jetAudio!")
        }
    }

    private fun checkBinding(): Boolean {
        if (appWidgetManager.getAppWidgetInfo(appWidgetId) != null) {
            appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            return true
        } else {
            return false
        }
    }

    fun convertToIcon() {
        val icon = Icon(parentActivity, null)
        val launchInfo = LaunchInfo()
        icon.setLaunchInfo(launchInfo)
        icon.setBlankOnDrag(true)
        val params = this.layoutParams as HomeLayout.LayoutParams
        params.columnSpan = 1
        params.rowSpan = 1
        icon.layoutParams = params
        parentLayout.addView(icon, params)
        this.removeAllViews()
        parentLayout.removeView(this)
        listener.onWidgetChanged()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val params = this.layoutParams as HomeLayout.LayoutParams
        params.rowSpan = parentLayout.heightToCells(neededHeight)
        params.columnSpan = parentLayout.widthToCells(neededWidth)
        this.layoutParams = params
        setMeasuredDimension(neededWidth, neededHeight)
    }

    interface WidgetInterface {
        fun needPermissionToBind(widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo)
        fun onWidgetChanged()
        fun configureWidget(widgetId: Int, widgetProviderInfo: AppWidgetProviderInfo)
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
        longClick()
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }
}