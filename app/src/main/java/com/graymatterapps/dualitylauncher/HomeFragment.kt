package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.viewpager2.widget.ViewPager2
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HomeFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var homePagerAdapter: HomePagerAdapter

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        settingsPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dock.depersistDock()
        dock.populateDock()
        dock.setListener(activity as MainActivity)

        homePagerAdapter = HomePagerAdapter(context as AppCompatActivity)
        homePager.adapter = homePagerAdapter
        homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                setupHomePageIndicator()
            }
        })
        homePagerAdapter.setListener(mainContext as HomePagerAdapter.HomeIconsInterface)

        homePager.adapter?.notifyDataSetChanged()

        setupHomePageIndicator()

        homeDelete.setColorFilter(
            colorPrefToColor(
                settingsPreferences.getString(
                    "home_widget_color",
                    "White"
                )
            )
        )
        homeDelete.alpha = 0f

        homeDelete.setOnDragListener(object : View.OnDragListener {
            override fun onDrag(view: View?, dragEvent: DragEvent?): Boolean {
                if (dragEvent != null) {
                    when (dragEvent.action) {
                        DragEvent.ACTION_DRAG_STARTED -> {
                            homeDelete.alpha = 1.0f
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            homeDelete.setBackgroundColor(Color.GREEN)
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            homeDelete.setBackgroundColor(Color.TRANSPARENT)
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            homeDelete.alpha = 0f
                        }
                        DragEvent.ACTION_DROP -> {
                            homeDelete.alpha = 0f
                            homeDelete.setBackgroundColor(Color.TRANSPARENT)
                            if (dragEvent.clipDescription.label.toString().equals("widget")) {
                                val widgetId: Int =
                                    Integer.parseInt(dragEvent.clipData.getItemAt(0).toString())
                                appWidgetHost.deleteAppWidgetId(widgetId)
                            }
                        }
                    }
                }
                return true
            }
        })

        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun setupHomePageIndicator() {
        homePageIndicator.removeAllViews()
        var params = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val filterColor = colorPrefToColor(
            settingsPreferences.getString(
                "home_widget_color",
                "White"
            )
        )

        for (n in 1..homePagerAdapter.itemCount) {
            var pageIndicator = ImageView(context)
            pageIndicator.layoutParams = params
            if (homePager.currentItem == n - 1) {
                pageIndicator.setImageResource(R.drawable.pager_active)
            } else {
                pageIndicator.setImageResource(R.drawable.pager_inactive)
            }
            pageIndicator.setColorFilter(filterColor)
            homePageIndicator.addView(pageIndicator)
        }
    }

    fun persistGrid(position: Int) {
        var homeIconsGrid = HomeIconsGrid()
        val view = homePager.findViewWithTag<View>(position)
        val homeIconsTable = view.findViewById<TableLayout>(R.id.homeIconsTable)
        for (y in 0 until homeIconsTable.childCount) {
            val dockRow: TableRow = homeIconsTable.getChildAt(y) as TableRow
            for (x in 0 until dockRow.childCount) {
                val icon: Icon = dockRow.getChildAt(x) as Icon
                homeIconsGrid.change(x, y, icon.getLaunchInfo())
            }
        }
        val saveItJson = Json.encodeToString(homeIconsGrid)
        val editor = prefs.edit()
        editor.putString("homeIconsGrid" + position, saveItJson)
        editor.apply()
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {

        if (key != null) {
            if (key.contains("homeIconsGrid")) {
                homePagerAdapter.notifyDataSetChanged()
            }
            if (key == "apps") {
                if(dock != null){
                    dock.populateDock()
                }
            }

            if (key == "home_grid_pages" || key == "home_grid_columns" || key == "home_grid_rows") {
                homePagerAdapter.notifyDataSetChanged()
                setupHomePageIndicator()
            }
            if (key == "home_widget_color") {
                setupHomePageIndicator()
                homeDelete.setColorFilter(
                    colorPrefToColor(
                        settingsPreferences.getString(
                            "home_widget_color",
                            "White"
                        )
                    )
                )
            }
            if (key == "home_text_color") {
                homePagerAdapter.notifyDataSetChanged()
            }
            if (key == "widget_visible") {
                widgetVisible()
            }
        }
    }

    fun startDrag(view: View, clipData: ClipData) {
        val dsb = View.DragShadowBuilder(view)
        view.startDrag(clipData, dsb, view, 0)
    }

    fun addWidget(appWidgetId: Int, appWidgetProviderInfo: AppWidgetProviderInfo, position: Int) {
        val view = homePager.findViewWithTag<View>(position)
        val widgetLayout = view.findViewById<GridLayout>(R.id.widgetLayout)
        val widgetContainer = activity?.applicationContext?.let {
            WidgetContainer(
                it,
                appWidgetId,
                appWidgetProviderInfo
            )
        }
        widgetLayout.addView(widgetContainer)
    }

    fun widgetVisible() {
        val view = homePager.findViewWithTag<View>(homePager.currentItem)
        val widgetLayout = view.findViewById<GridLayout>(R.id.widgetLayout)
        if (settingsPreferences.getBoolean("widget_visible", false)) {
            widgetLayout.visibility = View.VISIBLE
        } else {
            widgetLayout.visibility = View.INVISIBLE
        }
    }

    fun getCurrentHomePagerItem(): Int {
        return homePager.currentItem
    }
}