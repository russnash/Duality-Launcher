package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHost
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.graymatterapps.graymatterutils.GrayMatterUtils.shortToast

class SettingsDeveloper : PreferenceFragmentCompat() {

    lateinit var listener: DeveloperInterface
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_developer, rootKey)
        listener = mainContext as DeveloperInterface

        preferenceManager.findPreference<Preference>("update_app_list")?.setOnPreferenceClickListener {
            listener.updateAppList()
            true
        }

        preferenceManager.findPreference<Preference>("clear_icon_grid")?.setOnPreferenceClickListener {
            val editor = prefs.edit()
            editor.remove("homeIconsGrid0")
            editor.remove("homeIconsGrid1")
            editor.remove("homeIconsGrid2")
            editor.remove("homeIconsGrid3")
            editor.remove("homeIconsGrid4")
            editor.apply()
            shortToast(mainContext, "Home icon grid persistence cleared...")
            true
        }

        preferenceManager.findPreference<Preference>("clear_dock")?.setOnPreferenceClickListener {
            val editor = prefs.edit()
            editor.remove("dockItems")
            editor.apply()
            shortToast(mainContext, "Dock persistence cleared...")
            true
        }

        preferenceManager.findPreference<Preference>("clear_widget_grid")?.setOnPreferenceClickListener {
            val editor = prefs.edit()
            editor.remove("homeWidgetsGrid0")
            editor.remove("homeWidgetsGrid1")
            editor.remove("homeWidgetsGrid2")
            editor.remove("homeWidgetsGrid3")
            editor.remove("homeWidgetsGrid4")
            editor.apply()
            shortToast(mainContext, "Home widget grid persistence cleared...")
            true
        }

        preferenceManager.findPreference<Preference>("clear_appwidgethosts")?.setOnPreferenceClickListener {
            AppWidgetHost.deleteAllHosts()
            shortToast(mainContext, "AppWidgetHost data for Duality Launcher cleared...")
            true
        }
    }

    interface DeveloperInterface {
        fun updateAppList()
    }
}