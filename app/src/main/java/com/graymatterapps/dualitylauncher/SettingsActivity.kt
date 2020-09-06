package com.graymatterapps.dualitylauncher

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.graymatterapps.graymatterutils.GrayMatterUtils.getVersionCode
import com.graymatterapps.graymatterutils.GrayMatterUtils.getVersionName
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    var settingsFragment = SettingsFragment()
    lateinit var versionName: TextView
    lateinit var versionCode: TextView
    val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        versionName = findViewById(R.id.versionName)
        versionName.text = "VersionName: ${getVersionName(this)}"
        versionCode = findViewById(R.id.versionCode)
        versionCode.text = "VersionCode: ${getVersionCode(this)}"

        if(intent.getStringExtra("setting").equals("wallpaper")){
            val settingsWallpaperFragment = SettingsWallpaperFragment()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameLayout, settingsWallpaperFragment, "wallpaper")
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameLayout, settingsFragment, "settings")
                .commit()
        }

        supportActionBar?.title = "Duality Settings"
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val args = pref?.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment)
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
        supportActionBar?.title = pref.title
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
        //return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val frag = supportFragmentManager.findFragmentById(R.id.frameLayout)
        if(frag is SettingsFragment) {
            finish()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameLayout, settingsFragment, "settings")
                .commit()
            supportActionBar?.title = "Duality Settings"
        }
    }
}