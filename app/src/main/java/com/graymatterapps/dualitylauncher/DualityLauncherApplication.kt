package com.graymatterapps.dualitylauncher

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.customview.widget.ExploreByTouchHelper
import androidx.preference.PreferenceManager
import org.acra.*
import org.acra.annotation.*
import org.acra.data.StringFormat

lateinit var appWidgetHost: AppWidgetHost
lateinit var appWidgetManager: AppWidgetManager
lateinit var settingsPreferences: SharedPreferences
lateinit var prefs: SharedPreferences
lateinit var appList: AppList

@AcraCore(buildConfigClass = org.acra.BuildConfig::class, reportFormat= StringFormat.JSON)
@AcraMailSender(mailTo = "russnash37@gmail.com", reportAsFile = true)
@AcraDialog(resText = R.string.acra_dialog_text)
class DualityLauncherApplication: Application() {
    val TAG = javaClass.simpleName

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        ACRA.DEV_LOGGING = true
        ACRA.init(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    override fun onCreate() {
        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        appList = AppList(applicationContext)
        appWidgetHost = AppWidgetHost(applicationContext, 0)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetHost.startListening()
        super.onCreate()
    }

    override fun onTerminate() {
        appWidgetHost.stopListening()
        super.onTerminate()
    }
}