package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat

class WidgetContainer(
    val parentActivity: MainActivity,
    var appWidgetId: Int,
    private var appWidgetProviderInfo: AppWidgetProviderInfo
) : FrameLayout(parentActivity), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var parentLayout: HomeLayout
    var neededWidth: Int = 0
    var neededHeight: Int = 0
    private lateinit var listener: WidgetInterface
    private lateinit var gestureDetector: GestureDetectorCompat
    val TAG = javaClass.simpleName

    init {
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        parentLayout = this.parent as HomeLayout
        updateWidgetBorder()
        gestureDetector = GestureDetectorCompat(
            parentActivity,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }

                override fun onLongPress(e: MotionEvent?) {
                    longClick()
                }
            })
    }

    private fun updateWidgetBorder() {
        if (settingsPreferences.getBoolean("widget_borders", false)) {
            this.background = resources.getDrawable(R.drawable.widget_border)
        } else {
            this.background = ColorDrawable(Color.TRANSPARENT)
        }
        this.bringToFront()
    }

    fun setListener(ear: WidgetInterface) {
        listener = ear
    }

    private fun longClick() {
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

    fun addWidgetView() {
        appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        listener.updateWidgets(appWidgetProviderInfo)
        val widgetIndex = widgetDB.getWidgetIndex(appWidgetId)
        widgetDB.widgets[widgetIndex].appWidgetHostView.setPadding(0, 0, 0, 0)
        val minWidth =
            appWidgetProviderInfo.minWidth + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingLeft + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingRight
        val minHeight =
            appWidgetProviderInfo.minHeight + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingTop + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingBottom

        neededWidth = parentLayout.widthToCells(minWidth) * parentLayout.getCellWidth()
        neededHeight = parentLayout.heightToCells(minHeight) * parentLayout.getCellHeight()

        val containerParams = this.layoutParams as HomeLayout.LayoutParams
        containerParams.width = neededWidth
        containerParams.height = neededHeight
        containerParams.rowSpan = parentLayout.heightToCells(neededHeight)
        containerParams.columnSpan = parentLayout.widthToCells(neededWidth)
        this.layoutParams = containerParams

        val viewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        viewParams.width = neededWidth
        viewParams.height = neededHeight
        viewParams.gravity = Gravity.CENTER
        widgetDB.widgets[widgetIndex].appWidgetHostView.layoutParams = viewParams
        this.removeAllViews()
        try {
            val oldContainer =
                widgetDB.widgets[widgetIndex].appWidgetHostView.parent as WidgetContainer
            oldContainer.pleaseRemove(widgetDB.widgets[widgetIndex].appWidgetHostView)
        } catch (e: Exception) {
            Log.d(TAG, "Attempt at pleaseRemove() failed!")
        }
        this.addView(
            widgetDB.widgets[widgetIndex].appWidgetHostView,
            viewParams
        )
        val widthSpec = MeasureSpec.makeMeasureSpec(neededWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(neededHeight, MeasureSpec.EXACTLY)
        widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView.measure(
            widthSpec,
            heightSpec
        )
        widgetDB.widgets[widgetIndex].appWidgetHostView.updateAppWidgetSize(
            null,
            neededWidth,
            neededHeight,
            neededWidth,
            neededHeight
        )
        listener.onWidgetChanged()
        this.bringToFront()
    }

    fun pleaseRemove(widget: AppWidgetHostView) {
        this.removeView(widget)
    }

    private fun convertToIcon() {
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val i = widgetDB.allocateWidget(appWidgetId, appWidgetProviderInfo, this)
        if(this.childCount == 0) {
            addWidgetView()
        }
        val widthSpec = MeasureSpec.makeMeasureSpec(neededWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(neededHeight, MeasureSpec.EXACTLY)
        widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView.measure(
            widthSpec,
            heightSpec
        )
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        parentLayout = this.parent as HomeLayout
        val params = this.layoutParams as HomeLayout.LayoutParams
        params.rowSpan = parentLayout.heightToCells(neededHeight)
        params.columnSpan = parentLayout.widthToCells(neededWidth)
        this.layoutParams = params
        setMeasuredDimension(neededWidth, neededHeight)
    }

    interface WidgetInterface {
        fun onWidgetChanged()
        fun updateWidgets(widgetInfo: AppWidgetProviderInfo)
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        if (key != null) {
            if (key == "widget_borders") {
                updateWidgetBorder()
            }
        }
    }
}