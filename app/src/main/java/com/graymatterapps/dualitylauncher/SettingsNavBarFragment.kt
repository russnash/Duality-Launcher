package com.graymatterapps.dualitylauncher

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsNavBarFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_nav_bar, rootKey)
    }
}