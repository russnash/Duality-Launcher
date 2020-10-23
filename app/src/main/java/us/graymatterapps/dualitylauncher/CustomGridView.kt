package us.graymatterapps.dualitylauncher

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

class CustomGridView : GridView {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSpec: Int
        heightSpec = if (layoutParams.height == LayoutParams.WRAP_CONTENT) {
            // The great Android "hackatlon", the love, the magic.
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            MeasureSpec.makeMeasureSpec(
                Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST
            )
        } else {
            // Any other height should be respected as is.
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}