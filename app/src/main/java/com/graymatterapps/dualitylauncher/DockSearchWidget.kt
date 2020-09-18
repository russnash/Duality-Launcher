package com.graymatterapps.dualitylauncher

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.graymatterapps.graymatterutils.GrayMatterUtils
import kotlinx.android.synthetic.main.dock_search_widget.view.*

class DockSearchWidget(val con: Context, attributeSet: AttributeSet? = null) :
    LinearLayout(con, attributeSet),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val dockSearchLayout: ConstraintLayout
    private val dockSearchIcon: ImageView
    private val dockSearchText: EditText
    private val dockSpeakIcon: ImageView
    private val displayId: Int

    init {
        inflate(con, R.layout.dock_search_widget, this)
        dockSearchLayout = findViewById(R.id.dockSearchLayout)
        dockSearchIcon = findViewById(R.id.dockSearchIcon)
        dockSearchText = findViewById(R.id.dockSearchText)
        dockSpeakIcon = findViewById(R.id.dockSpeakIcon)
        setColorScheme()

        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayId = wm.defaultDisplay.displayId

        dockSearchText.setOnKeyListener { view, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (dockSearchText.text.toString() != "") {
                    launchSearch(dockSearchText.text.toString())
                } else {
                    GrayMatterUtils.hideKeyboardFrom(con, dockSearchText)
                }
                dockSearchText.text.clear()
                dockSearchText.clearFocus()
            }
            true
        }

        dockSearchIcon.setOnClickListener {
            val launchInfo = LaunchInfo("com.google.android.googlequicksearchbox.SearchActivity", "com.google.android.googlequicksearchbox", 0, LaunchInfo.ICON)
            try {
                appList.launchPackage(launchInfo, displayId)
            } catch (e: Exception) {
                GrayMatterUtils.shortToast(con, "Could not launch Google Now")
            }
        }

        dockSpeakIcon.setOnClickListener {
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = displayId
            val intent = Intent(Intent.ACTION_VOICE_COMMAND)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                con.startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                GrayMatterUtils.shortToast(con, "Could not launch Google Assistant!")
            }
        }
    }

    fun clearSearchFocus() {
        dockSearchText.clearFocus()
    }

    private fun setColorScheme() {
        dockSearchLayout.backgroundTintList =
            ColorStateList.valueOf(settingsPreferences.getInt("dock_search_color", Color.BLACK))
    }

    private fun launchSearch(terms: String) {
        val provider = settingsPreferences.getString("dock_search_provider", "Google")
        var prefix: String = ""

        prefix = when (provider) {
            "Google" -> {
                "https://www.google.com/search?q="
            }
            "Yahoo" -> {
                "https://search.yahoo.com/search?p="
            }
            "Google (App)" -> {
                ""
            }
            else -> {
                "https://www.google.com/search?q="
            }
        }
        if (provider == "Google (App)") {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.setPackage("com.google.android.googlequicksearchbox")
            intent.putExtra(SearchManager.QUERY, terms)
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = displayId
            try {
                con.startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                GrayMatterUtils.shortToast(con, "Google App not found!")
            }
        } else {
            val url = prefix + terms
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = displayId
            try {
                con.startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                GrayMatterUtils.shortToast(con, "Could not launch default browser!")
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        if (key != null) {
            if (key == "dock_search_color") {
                setColorScheme()
            }
        }
    }
}