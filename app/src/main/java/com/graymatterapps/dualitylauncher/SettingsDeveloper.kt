package com.graymatterapps.dualitylauncher

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsDeveloper : PreferenceFragmentCompat() {

    lateinit var listener: DeveloperInterface

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_developer, rootKey)
        listener = mainContext as DeveloperInterface

        preferenceManager.findPreference<Preference>("update_app_list")?.setOnPreferenceClickListener {
            listener.updateAppList()
            true
        }
    }

    interface DeveloperInterface {
        fun updateAppList()
    }
}