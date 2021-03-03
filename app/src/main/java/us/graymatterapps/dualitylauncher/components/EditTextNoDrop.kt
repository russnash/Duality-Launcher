package us.graymatterapps.dualitylauncher.components

import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent

class EditTextNoDrop(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {
    override fun onDragEvent(event: DragEvent?): Boolean {
        return false
    }
}