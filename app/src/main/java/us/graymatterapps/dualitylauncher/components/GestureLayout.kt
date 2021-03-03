package us.graymatterapps.dualitylauncher.components

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import us.graymatterapps.dualitylauncher.swiping

class GestureLayout(context: Context, attributeSet: AttributeSet) :
    ConstraintLayout(context, attributeSet) {

    lateinit var listener: GestureEvents
    private var gesturesOn: Boolean = false
    private var drawerOpen: Boolean = false
    private var dialogOpen: Boolean = false
    private val touchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    val TAG = javaClass.simpleName

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (gesturesOn) {
            if (event != null) {
                when (event.action) {
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.historySize != 0) {
                            val swipeUpDistance = event.getHistoricalY(0) - event.y
                            val swipeDownDistance = event.y - event.getHistoricalY(0)
                            val swipeLeftDistance = event.getHistoricalX(0) - event.x
                            val swipeRightDistance = event.x - event.getHistoricalX(0)

                            if (swipeLeftDistance > touchSlop) {
                                swiping = true
                                return false
                            }
                            if (swipeRightDistance > touchSlop) {
                                swiping = true
                                return false
                            }
                            if (swipeUpDistance > touchSlop) {
                                swiping = true
                                listener.onSwipeUp()
                                return !drawerOpen
                            }
                            if (swipeDownDistance > touchSlop) {
                                swiping = true
                                listener.onSwipeDown()
                                return !drawerOpen
                            }
                        }
                        return false
                    }
                    else -> {
                        return false
                    }
                }
            }
        } else {
            Log.d(TAG, "Gestures are off!!!")
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

    fun setGesturesOn(state: Boolean) {
        gesturesOn = state
    }

    fun setDrawerOpen(state: Boolean) {
        drawerOpen = state
    }

    fun setDialogOpen(state: Boolean) {
        dialogOpen = state
    }
}