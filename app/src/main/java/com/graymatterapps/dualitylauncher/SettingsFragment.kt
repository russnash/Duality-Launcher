package com.graymatterapps.dualitylauncher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.graymatterapps.graymatterutils.GrayMatterUtils
import java.lang.Exception

class SettingsFragment : PreferenceFragmentCompat() {
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey)

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
                editor.apply()
            } catch (e: Exception) {
                GrayMatterUtils.longToast(appContext, "Could not extract colors from wallpaper!")
            }
            true
        }
    }
}