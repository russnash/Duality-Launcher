package com.graymatterapps.dualitylauncher

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class GestureLayout(context: Context, attributeSet: AttributeSet): LinearLayout(context, attributeSet) {

    lateinit var listener: GestureEvents
    private var areGesturesOn: Boolean = false
    private val touchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if(areGesturesOn) {
            if (event != null) {
                when (event.action) {
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if(event.historySize != 0) {
                            val swipeUpDistance = event.getHistoricalY(0) - event.y
                            val swipeDownDistance = event.y - event.getHistoricalY(0)
                            val swipeLeftDistance = event.getHistoricalX(0) - event.x
                            val swipeRightDistance = event.x - event.getHistoricalX(0)

                            if(swipeLeftDistance > touchSlop){
                                return false
                            }
                            if(swipeRightDistance > touchSlop){
                                return false
                            }
                            if(swipeUpDistance > touchSlop) {
                                listener.onSwipeUp()
                                return true
                            }
                            if(swipeDownDistance > touchSlop) {
                                listener.onSwipeDown()
                                return true
                            }
                        }
                        return false
                    }
                    else -> {
                        return false
                    }
                }
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    interface GestureEvents {
        fun onSwipeUp()
        fun onSwipeDown()
    }

    fun setListener(activity: Activity) {
        listener = activity as GestureEvents
    }

    fun setGesturesOn(state: Boolean){
        areGesturesOn = state
    }
}