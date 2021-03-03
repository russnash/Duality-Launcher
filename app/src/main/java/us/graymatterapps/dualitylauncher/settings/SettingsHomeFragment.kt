package us.graymatterapps.dualitylauncher.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import us.graymatterapps.dualitylauncher.*
import us.graymatterapps.graymatterutils.GrayMatterUtils
import java.lang.Exception

class SettingsHomeFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_home, rootKey)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)

        preferenceManager.findPreference<Preference>("full_screen_mode")
            ?.setOnPreferenceClickListener {
                val editor = settingsPreferences.edit()
                val color = ColorUtils.setAlphaComponent(Color.BLACK, 0)
                editor.putInt("status_background", color)
                editor.putInt("nav_background", color)
                editor.apply()
                true
            }

        preferenceManager.findPreference<Preference>("auto_color")?.setOnPreferenceClickListener {
            val wallpaperManager =
                requireActivity().getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
            val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            val editor = settingsPreferences.edit()
            try {
                val primaryColor = ColorUtils.setAlphaComponent(
                    colors!!.primaryColor.toArgb(),
                    settingsPreferences.getInt("auto_color_alpha", 200)
                )
                editor.putInt("dock_background_color", primaryColor)
                editor.putInt("dock_search_color", primaryColor)
                editor.putInt("folder_background", primaryColor)
                editor.putInt("app_drawer_background", primaryColor)
                editor.putInt("folder_icon_background_color", primaryColor)
                editor.apply()
            } catch (e: Exception) {
                GrayMatterUtils.longToast(appContext, "Could not extract colors from wallpaper!")
            }
            true
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
        if (preference != null) {
            if (preference is ColorPreference) {
                val color = sharedPreferences?.getInt(preference.key, Color.BLACK)
                if (color != null) {
                    preference.summary = GrayMatterUtils.colorToColorPref(color)
                    preference.icon = ColorDrawable(color)
                }
            }
        }
        if (key == "manual_color_scheme") {
            val editor = settingsPreferences.edit()
            val primaryColor = settingsPreferences.getInt("manual_color_scheme", Color.BLACK)
            editor.putInt("dock_background_color", primaryColor)
            editor.putInt("dock_search_color", primaryColor)
            editor.putInt("folder_background", primaryColor)
            editor.putInt("app_drawer_background", primaryColor)
            editor.putInt("folder_icon_background_color", primaryColor)
            editor.apply()
        }
    }
}