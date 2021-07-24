package us.graymatterapps.dualitylauncher.settings

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import us.graymatterapps.dualitylauncher.*
import us.graymatterapps.graymatterutils.GrayMatterUtils

class SettingsIcon : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val iconPackManager = dualityLauncherApplication.getIconPackManagerContext()
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_icon, rootKey)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)

        var chooseIconPack = preferenceManager.findPreference<ListPreference>("choose_icon_pack")
        var entries = ArrayList<String>()
        var entriesValues = ArrayList<String>()
        entries.add("Default")
        entriesValues.add("Default")
        iconPackManager.iconPacks.forEach{
            entries.add(it.name)
            entriesValues.add(it.name)
        }
        if (chooseIconPack != null) {
            chooseIconPack.entries = entries.toArray(arrayOfNulls<CharSequence>(entries.size))
            chooseIconPack.entryValues = entriesValues.toArray(arrayOfNulls<CharSequence>(entriesValues.size))
            chooseIconPack.setDefaultValue("Default")
        }
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