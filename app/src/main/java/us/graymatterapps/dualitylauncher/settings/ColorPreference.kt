package us.graymatterapps.dualitylauncher.settings

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import androidx.preference.DialogPreference
import us.graymatterapps.dualitylauncher.R
import us.graymatterapps.graymatterutils.GrayMatterUtils

class ColorPreference @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = defStyleAttr
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    var prefVal: Int = DEFAULT_VALUE
        set(value) {
            field = value
            persistInt(value)
        }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Int {
        return a?.getInt(index, DEFAULT_VALUE) ?: DEFAULT_VALUE
    }

    override fun getDialogLayoutResource() = R.layout.color_chooser

    override fun onSetInitialValue(defaultValue: Any?) {
        prefVal = getPersistedInt((defaultValue as? Int) ?: DEFAULT_VALUE)
        setPositiveButtonText(R.string.ok)
        setNegativeButtonText(R.string.cancel)
    }

    override fun getSummary(): CharSequence {
        return GrayMatterUtils.colorToColorPref(prefVal)
    }

    companion object {
        private const val DEFAULT_VALUE = Color.BLACK
    }
}