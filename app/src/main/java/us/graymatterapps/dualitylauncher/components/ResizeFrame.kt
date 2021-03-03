package us.graymatterapps.dualitylauncher.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import us.graymatterapps.dualitylauncher.R

open class ResizeFrame(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private lateinit var widgetResize: ConstraintLayout
    private lateinit var resizeBorder: ImageView
    private lateinit var resizeTop: ImageView
    private lateinit var resizeTopMinus: ImageView
    private lateinit var resizeBottom: ImageView
    private lateinit var resizeBottomMinus: ImageView
    private lateinit var resizeLeft: ImageView
    private lateinit var resizeLeftMinus: ImageView
    private lateinit var resizeRight: ImageView
    private lateinit var resizeRightMinus: ImageView
    private lateinit var listener: ResizeInterface

    init {
        inflateLayout()
    }

    fun inflateLayout() {
        inflate(context, R.layout.resize_frame, this)
        widgetResize = findViewById(R.id.widgetResize)
        resizeBorder = findViewById(R.id.resizeBorder)
        resizeTop = findViewById(R.id.resizeTop)
        resizeBottom = findViewById(R.id.resizeBottom)
        resizeLeft = findViewById(R.id.resizeLeft)
        resizeRight = findViewById(R.id.resizeRight)
        resizeTopMinus = findViewById(R.id.resizeTopMinus)
        resizeBottomMinus = findViewById(R.id.resizeBottomMinus)
        resizeLeftMinus = findViewById(R.id.resizeLeftMinus)
        resizeRightMinus = findViewById(R.id.resizeRightMinus)
        widgetResize.visibility = View.INVISIBLE

        resizeTop.setOnClickListener {
            listener.onTopPlus()
        }
        resizeBottom.setOnClickListener {
            listener.onBottomPlus()
        }
        resizeLeft.setOnClickListener {
            listener.onLeftPlus()
        }
        resizeRight.setOnClickListener {
            listener.onRightPlus()
        }
        resizeTopMinus.setOnClickListener {
            listener.onTopMinus()
        }
        resizeBottomMinus.setOnClickListener {
            listener.onBottomMinus()
        }
        resizeLeftMinus.setOnClickListener {
            listener.onLeftMinus()
        }
        resizeRightMinus.setOnClickListener {
            listener.onRightMinus()
        }
    }

    fun setResize(state: Boolean) {
        if (state) {
            widgetResize.visibility = View.VISIBLE
            this.bringToFront()
        } else {
            widgetResize.visibility = View.INVISIBLE
        }
    }

    fun isResizing(): Boolean {
        return widgetResize.visibility == View.VISIBLE
    }

    fun setListener(ear: ResizeInterface) {
        listener = ear
    }

    interface ResizeInterface {
        fun onTopPlus() {

        }

        fun onBottomPlus() {

        }

        fun onLeftPlus() {

        }

        fun onRightPlus() {

        }

        fun onTopMinus() {

        }

        fun onBottomMinus() {

        }

        fun onLeftMinus() {

        }

        fun onRightMinus() {

        }
    }
}