package us.graymatterapps.dualitylauncher

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import us.graymatterapps.graymatterutils.GrayMatterUtils.getVersionCode
import us.graymatterapps.graymatterutils.GrayMatterUtils.getVersionName
import kotlinx.android.synthetic.main.activity_main.*
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
        val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        findViewById<FrameLayout>(R.id.frameLayout).startAnimation(animation)
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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameLayout, settingsFragment, "settings")
                .commit()
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
            findViewById<FrameLayout>(R.id.frameLayout).startAnimation(animation)
            supportActionBar?.title = "Duality Settings"
        }
    }
}