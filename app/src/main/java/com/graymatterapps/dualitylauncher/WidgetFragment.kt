package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils

class WidgetFragment : Fragment(), WidgetChooserAdapter.WidgetChooserInterface {

    lateinit var installedProvs: MutableList<AppWidgetProviderInfo>
    lateinit var widgetChooser: RecyclerView
    lateinit var widgetChooserAdapter: RecyclerView.Adapter<*>
    lateinit var widgetChooserManager: RecyclerView.LayoutManager
    private lateinit var listener: WidgetInterface
    lateinit var widgetPreviewImage: Drawable
    private var appWidgetId: Int = 0
    private lateinit var appWidgetProviderInfo: AppWidgetProviderInfo
    val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installedProvs = appWidgetManager.installedProviders
        installedProvs.sortBy {
            it.provider.className
        }
        listener = activity as WidgetInterface
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_widget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        widgetChooser = view.findViewById(R.id.widgetChooser)
        widgetChooserAdapter = WidgetChooserAdapter(mainContext, installedProvs)
        (widgetChooserAdapter as WidgetChooserAdapter).setListener(this)
        widgetChooserManager = LinearLayoutManager(mainContext)
        widgetChooser.layoutManager = widgetChooserManager
        widgetChooser.adapter = widgetChooserAdapter

        var color = settingsPreferences.getInt(
            "app_drawer_background",
            Color.BLACK
        )
        widgetChooser.setBackgroundColor(color)
    }

    override fun onWidgetChosen(position: Int, view: View) {
        appWidgetProviderInfo = installedProvs[position]
        appWidgetId = appWidgetHost.allocateAppWidgetId()
        val id = System.currentTimeMillis().toString()
        val widgetInfo = WidgetInfo(appWidgetId, appWidgetProviderInfo, view)
        MainActivity.dragAndDropData.addWidget(widgetInfo, id)
        val clipData = ClipData.newPlainText("widget", id)
        listener.onAddWidget(clipData, view)
    }

    interface WidgetInterface {
        fun onAddWidget(
            clipData: ClipData,
            view: View
        )
    }
}