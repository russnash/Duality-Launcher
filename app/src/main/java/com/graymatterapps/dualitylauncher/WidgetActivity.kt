package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.customview.widget.ExploreByTouchHelper.HOST_ID
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.dualitylauncher.MainActivity.Companion.appList
import com.graymatterapps.dualitylauncher.MainActivity.Companion.context

class WidgetActivity : AppCompatActivity(), WidgetChooserAdapter.WidgetChooserInterface {
    val appWidgetHost = AppWidgetHost(context, HOST_ID)
    val appWidgetManager = AppWidgetManager.getInstance(context)
    lateinit var installedProvs: MutableList<AppWidgetProviderInfo>
    lateinit var widgetChooser: RecyclerView
    lateinit var widgetChooserAdapter: RecyclerView.Adapter<*>
    lateinit var widgetChooserManager: RecyclerView.LayoutManager
    private var appWidgetId: Int = 0
    private lateinit var info: AppWidgetProviderInfo
    private lateinit var listener: WidgetInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        installedProvs = appWidgetManager.installedProviders
        installedProvs.sortBy {
            it.provider.packageName
        }
        widgetChooser = findViewById(R.id.widgetChooser)
        widgetChooserAdapter = WidgetChooserAdapter(this, installedProvs)
        widgetChooserManager = LinearLayoutManager(this)
        widgetChooser.layoutManager = widgetChooserManager
        widgetChooser.adapter = widgetChooserAdapter

        var basicColor = MainActivity.colorPrefToColor(settingsPreferences.getString("app_drawer_background", "Black"))
        var alpha = settingsPreferences.getInt("app_drawer_background_alpha", 80)
        var backgroundColor = ColorUtils.setAlphaComponent(basicColor, alpha)
        widgetChooser.setBackgroundColor(backgroundColor)

        listener = mainContext as WidgetInterface
    }

    override fun onWidgetChosen(position: Int) {
        info = installedProvs[position]
        appWidgetId = appWidgetHost.allocateAppWidgetId()
        val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)

        if(!canBind){
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            startActivityForResult(intent, 1)
        } else {
            bindWidget()
        }
    }

    fun bindWidget() {
        val widgetView = appWidgetHost.createView(mainContext, appWidgetId, info)
        widgetView.setAppWidget(appWidgetId, info)
        listener.onWidgetBind(widgetView)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == RESULT_OK) {
            bindWidget()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    interface WidgetInterface {
        fun onWidgetBind(widgetView: AppWidgetHostView)
    }
}