package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor

//const val REQUEST_PERMISSION = 1
//const val CONFIGURE_WIDGET = 2

class WidgetActivity : AppCompatActivity(), WidgetChooserAdapter.WidgetChooserInterface {

    lateinit var installedProvs: MutableList<AppWidgetProviderInfo>
    lateinit var widgetChooser: RecyclerView
    lateinit var widgetChooserAdapter: RecyclerView.Adapter<*>
    lateinit var widgetChooserManager: RecyclerView.LayoutManager
    lateinit var widgetPreviewImage: Drawable
    private var appWidgetId: Int = 0
    private lateinit var appWidgetProviderInfo: AppWidgetProviderInfo
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

        var basicColor =
            colorPrefToColor(settingsPreferences.getString("app_drawer_background", "Black"))
        var alpha = settingsPreferences.getInt("app_drawer_background_alpha", 80)
        var backgroundColor = ColorUtils.setAlphaComponent(basicColor, alpha)
        widgetChooser.setBackgroundColor(backgroundColor)

        listener = mainContext as WidgetInterface
    }

    override fun onWidgetChosen(position: Int, view: View) {
        appWidgetProviderInfo = installedProvs[position]
        widgetPreviewImage = installedProvs[position].loadPreviewImage(mainContext, -1)
        appWidgetId = appWidgetHost.allocateAppWidgetId()
        val canBind =
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetProviderInfo.provider)

        if (!canBind) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetProviderInfo.provider)
            }
            startActivityForResult(intent, REQUEST_PERMISSION)
        } else {
            bindWidget()
        }
    }

    fun bindWidget() {
        if (appWidgetProviderInfo.configure != null) {
            intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.setComponent(appWidgetProviderInfo.configure)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            try {
                startActivityForResult(intent, CONFIGURE_WIDGET)
            } catch (e: Exception) {
                createWidget()
            }

        } else {
            createWidget()
        }
    }

    fun createWidget(data: Intent? = null) {
        if (data != null) {
            val extras = data.extras
            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            }
        }
        listener.onAddWidget(appWidgetId, appWidgetProviderInfo, widgetPreviewImage)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PERMISSION) {
                if (resultCode == RESULT_OK) {
                    bindWidget()
                }
            }

            if (requestCode == CONFIGURE_WIDGET) {
                createWidget(data)
            }
        } else {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    interface WidgetInterface {
        fun onAddWidget(widgetView: Int, appWidgetProviderInfo: AppWidgetProviderInfo, widgetPreviewImage: Drawable)
    }
}