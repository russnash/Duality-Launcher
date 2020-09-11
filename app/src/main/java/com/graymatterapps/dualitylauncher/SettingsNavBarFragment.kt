package com.graymatterapps.dualitylauncher

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.graymatterapps.graymatterutils.GrayMatterUtils

class SettingsNavBarFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_nav_bar, rootKey)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {

        val dialogFragment: DialogFragment? = when (preference) {
            is ColorPreference -> {
                ColorPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            }
            else -> null
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(
                requireFragmentManager(),
                "androidx.preference.PreferenceFragment.DIALOG"
            )
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val preference: Preference? = findPreference(key.toString())
        if(preference != null) {
            if(preference is ColorPreference) {
                val color = sharedPreferences?.getInt(preference.key, Color.BLACK)
                if (color != null) {
                    preference.summary = GrayMatterUtils.colorToColorPref(color)
                    preference.icon = ColorDrawable(color)
                }
            }
        }
    }
}