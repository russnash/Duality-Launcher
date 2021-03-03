package us.graymatterapps.dualitylauncher.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import us.graymatterapps.dualitylauncher.R

class SettingsFragment : PreferenceFragmentCompat() {
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey)
    }
}