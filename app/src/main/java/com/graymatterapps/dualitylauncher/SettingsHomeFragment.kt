package com.graymatterapps.dualitylauncher

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsHomeFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_home, rootKey)
    }
}