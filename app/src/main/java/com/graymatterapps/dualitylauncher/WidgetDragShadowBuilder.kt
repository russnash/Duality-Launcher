package com.graymatterapps.dualitylauncher

import android.graphics.Point
import android.view.View

class WidgetDragShadowBuilder(view: View) : View.DragShadowBuilder(view) {

    override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
        if (outShadowSize != null) {
            outShadowSize.set(view.width, view.height)
        }
        if (outShadowTouchPoint != null) {
            outShadowTouchPoint.set(0, 0)
        }
    }

}