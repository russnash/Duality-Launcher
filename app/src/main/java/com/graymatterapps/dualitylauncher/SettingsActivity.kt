package com.graymatterapps.dualitylauncher

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    var settingsFragment = SettingsFragment()
    lateinit var version: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        version = findViewById(R.id.version)
        version.text = "Version: " + BuildConfig.VERSION_NAME

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayout, settingsFragment, "settings")
            .commit()
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