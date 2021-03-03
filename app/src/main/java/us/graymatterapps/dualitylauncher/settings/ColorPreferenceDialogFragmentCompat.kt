package us.graymatterapps.dualitylauncher.settings

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.preference.PreferenceDialogFragmentCompat
import us.graymatterapps.graymatterutils.GrayMatterUtils
import us.graymatterapps.dualitylauncher.R
import us.graymatterapps.dualitylauncher.settingsPreferences

class ColorPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat(),
    AdapterView.OnItemSelectedListener {
    var chosenColor: Int = Color.BLACK
    lateinit var seekBarRed: SeekBar
    lateinit var redLeft: ImageView
    lateinit var redRight: ImageView
    lateinit var seekBarGreen: SeekBar
    lateinit var greenLeft: ImageView
    lateinit var greenRight: ImageView
    lateinit var seekBarBlue: SeekBar
    lateinit var blueLeft: ImageView
    lateinit var blueRight: ImageView
    lateinit var seekBarAlpha: SeekBar
    lateinit var alphaLeft: ImageView
    lateinit var alphaRight: ImageView
    lateinit var spinnerPreset: Spinner
    lateinit var colorPreview: ImageView
    lateinit var valueRed: TextView
    lateinit var valueBlue: TextView
    lateinit var valueGreen: TextView
    lateinit var valueAlpha: TextView
    var firstRun: Boolean = true
    lateinit var colorArray: Array<String>

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        if (view != null) {
            seekBarRed = view.findViewById(R.id.seekBarRed)
            redLeft = view.findViewById(R.id.redLeft)
            redRight = view.findViewById(R.id.redRight)
            seekBarGreen = view.findViewById(R.id.seekBarGreen)
            greenLeft = view.findViewById(R.id.greenLeft)
            greenRight = view.findViewById(R.id.greenRight)
            seekBarBlue = view.findViewById(R.id.seekBarBlue)
            blueLeft = view.findViewById(R.id.blueLeft)
            blueRight = view.findViewById(R.id.blueRight)
            seekBarAlpha = view.findViewById(R.id.seekBarAlpha)
            alphaLeft = view.findViewById(R.id.alphaLeft)
            alphaRight = view.findViewById(R.id.alphaRight)
            spinnerPreset = view.findViewById(R.id.spinnerPreset)
            colorPreview = view.findViewById(R.id.colorPreview)
            valueRed = view.findViewById(R.id.valueRed)
            valueGreen = view.findViewById(R.id.valueGreen)
            valueBlue = view.findViewById(R.id.valueBlue)
            valueAlpha = view.findViewById(R.id.valueAlpha)
        }

        colorArray = resources.getStringArray(R.array.android_colors)

        seekBarRed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                valueRed.text = p1.toString()
                calcColor()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
        redLeft.setOnClickListener {
            seekBarRed.progress--
        }
        redRight.setOnClickListener {
            seekBarRed.progress++
        }
        seekBarGreen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                valueGreen.text = p1.toString()
                calcColor()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
        greenLeft.setOnClickListener {
            seekBarGreen.progress--
        }
        greenRight.setOnClickListener {
            seekBarGreen.progress++
        }
        seekBarBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                valueBlue.text = p1.toString()
                calcColor()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
        blueLeft.setOnClickListener {
            seekBarBlue.progress--
        }
        blueRight.setOnClickListener {
            seekBarBlue.progress++
        }
        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                valueAlpha.text = p1.toString()
                calcColor()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
        alphaLeft.setOnClickListener {
            seekBarAlpha.progress--
        }
        alphaRight.setOnClickListener {
            seekBarAlpha.progress++
        }
        var adapter = ArrayAdapter.createFromResource(
            requireActivity().baseContext,
            R.array.android_colors,
            R.layout.plain_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPreset.adapter = adapter
        spinnerPreset.onItemSelectedListener = this

        chosenColor = settingsPreferences.getInt(arguments?.get(ARG_KEY).toString(), 0)
        calcColor(chosenColor)
    }

    private fun calcColor(colorName: Int? = null) {
        if (colorName != null) {
            chosenColor = colorName
            setSliders()
        }
        val red = seekBarRed.progress
        val green = seekBarGreen.progress
        val blue = seekBarBlue.progress
        val alpha = seekBarAlpha.progress
        var final = Color.argb(alpha, red, green, blue)
        colorPreview.setImageDrawable(ColorDrawable(final))
        chosenColor = final
        spinnerPreset.setSelection(colorArray.indexOf(GrayMatterUtils.colorToColorPref(chosenColor)))
    }

    private fun setSliders() {
        val color = Color.valueOf(chosenColor)
        val red = (color.red() * 255).toInt()
        val green = (color.green() * 255).toInt()
        val blue = (color.blue() * 255).toInt()
        val alpha = (color.alpha() * 255).toInt()
        seekBarRed.progress = red
        seekBarGreen.progress = green
        seekBarBlue.progress = blue
        seekBarAlpha.progress = alpha
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            preference.apply {
                prefVal = chosenColor
            }
        }
    }

    override fun getPreference(): ColorPreference {
        return super.getPreference() as? ColorPreference
            ?: error("Preference is not a ColorPreference")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        if (!firstRun) {
            val selectedColor = parent!!.getItemAtPosition(pos).toString()
            if(selectedColor == "Custom"){
                calcColor()
            } else {
                calcColor(GrayMatterUtils.colorPrefToColor(selectedColor))
            }
        } else {
            firstRun = false
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing
    }

    companion object {
        fun newInstance(key: String?) = ColorPreferenceDialogFragmentCompat().apply {
            arguments = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
        }
    }
}