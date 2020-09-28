package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHost
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.graymatterapps.graymatterutils.GrayMatterUtils.shortToast
import com.graymatterapps.graymatterutils.GrayMatterUtils.showOkDialog

class SettingsDeveloper : PreferenceFragmentCompat() {

    lateinit var listener: DeveloperInterface
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_developer, rootKey)
        listener = generalContext as DeveloperInterface

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
            editor.putLong("notifyDataSetChanged", System.currentTimeMillis())
            editor.apply()
            shortToast(requireActivity(), "Home icon / folder grid persistence cleared...")
            true
        }

        preferenceManager.findPreference<Preference>("clear_dock")?.setOnPreferenceClickListener {
            val editor = prefs.edit()
            editor.remove("dockItems")
            editor.putLong("notifyDataSetChanged", System.currentTimeMillis())
            editor.apply()
            shortToast(requireActivity(), "Dock persistence cleared...")
            true
        }

        preferenceManager.findPreference<Preference>("clear_widget_grid")?.setOnPreferenceClickListener {
            val editor = prefs.edit()
            val allPrefs = prefs.all
            allPrefs.forEach{
                if(it.key.contains("homeWidgetsGrid")){
                    editor.remove(it.key)
                }
                if(it.key.contains("widgetSizes")) {
                    editor.remove(it.key)
                }
                widgetDB = WidgetDB(appContext)
            }
            editor.putLong("notifyDataSetChanged", System.currentTimeMillis())
            editor.apply()
            listener.removeWidgets()
            listener.showWidgets()
            shortToast(requireActivity(), "Home widget grid persistence and AppWidgetHost data cleared...")

            true
        }

        preferenceManager.findPreference<Preference>("clear_folders")?.setOnPreferenceClickListener {
            val editor = prefs.edit()
            val pairs = prefs.all
            pairs.forEach {
                if(it.key.startsWith("folder")) {
                    editor.remove(it.key)
                }
            }
            editor.apply()
            true
        }

        preferenceManager.findPreference<Preference>("log_recents")?.setOnPreferenceClickListener {
            listener.logRecents()
            true
        }
    }

    interface DeveloperInterface {
        fun updateAppList()
        fun logRecents()
        fun removeWidgets(leavePager: Boolean = false)
        fun showWidgets()
    }
}