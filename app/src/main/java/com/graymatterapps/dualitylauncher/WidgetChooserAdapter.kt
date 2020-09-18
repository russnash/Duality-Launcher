package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor

class WidgetChooserAdapter(
    private val context: Context,
    private val widgets: List<AppWidgetProviderInfo>
) : RecyclerView.Adapter<WidgetChooserAdapter.WidgetChooserHolder>() {

    private lateinit var listener: WidgetChooserInterface
    val TAG = javaClass.simpleName

    class WidgetChooserHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetChooserHolder {
        val inflatedView = parent.inflate(R.layout.widget_item, false)
        return WidgetChooserHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: WidgetChooserHolder, position: Int) {
        val rowView = holder.itemView
        val icon = rowView.findViewById<ImageView>(R.id.widgetIcon)
        val preview = rowView.findViewById<ImageView>(R.id.widgetPreview)
        val description = rowView.findViewById<TextView>(R.id.widgetDescription)
        val textColor = settingsPreferences.getInt("app_drawer_text", Color.WHITE)
        icon.setImageDrawable(widgets[position].loadIcon(context, -1))
        preview.setImageDrawable(widgets[position].loadPreviewImage(context, -1))
        description.text = widgets[position].loadLabel(context.packageManager)
        description.setTextColor(textColor)
        preview.setOnLongClickListener {
            listener.onWidgetChosen(position, it)
            true
        }
    }

    override fun getItemCount(): Int {
        return widgets.size
    }

    fun setListener(ear: WidgetChooserInterface) {
        listener = ear
    }

    interface WidgetChooserInterface {
        fun onWidgetChosen(position: Int, view: View)
    }
}