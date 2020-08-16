package com.graymatterapps.dualitylauncher

import android.content.Context
import android.graphics.Color
import android.view.DragEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.gridlayout.widget.GridLayout
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData

class WidgetPlaceholder(context: Context): View(context) {
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 80)
    private lateinit var parentLayout: GridLayout

    override fun onAttachedToWindow() {
        parentLayout = this.parent as GridLayout
        val cellWidth = parentLayout.width / 8
        val cellHeight = parentLayout.height / 8
        this.layoutParams.width = cellWidth
        this.layoutParams.height = cellHeight

        this.setOnDragListener { view, dragEvent ->
            var respondToDrag = false
            try{
                if(dragEvent.clipDescription.label.toString().equals("widget")){
                    respondToDrag = true
                }
            } catch (e: Exception) {
                respondToDrag = false
            }
            when(dragEvent.action){
                DragEvent.ACTION_DRAG_STARTED -> {
                    if(respondToDrag) {
                        this.setBackgroundResource(R.drawable.icon_drag_target)
                    }
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    if(respondToDrag) {
                        this.setBackgroundColor(enteredColor)
                    }
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    if(respondToDrag) {
                        this.setBackgroundResource(R.drawable.icon_drag_target)
                    }
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    this.setBackgroundColor(Color.TRANSPARENT)
                }
                DragEvent.ACTION_DROP -> {
                    if(respondToDrag) {
                        val parentLayout = this.parent as GridLayout
                        val appWidgetId = dragAndDropData.retrieveWidgetId(dragEvent.clipData.getItemAt(0).toString())
                        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                        val widgetContainer = WidgetContainer(context, appWidgetId, appWidgetProviderInfo)
                        val params = this.layoutParams as GridLayout.LayoutParams
                        parentLayout.addView(widgetContainer, params)
                    }
                }
            }
            true
        }
        super.onAttachedToWindow()
    }
}