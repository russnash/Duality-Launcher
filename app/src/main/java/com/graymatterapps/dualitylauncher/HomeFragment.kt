package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HomeFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var homePagerAdapter: HomePagerAdapter
    val TAG = "HomeFragment"

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

        homePager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        homePagerAdapter = HomePagerAdapter(context as AppCompatActivity, frameLayout)
        homePager.adapter = homePagerAdapter
        homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                setupHomePageIndicator()
            }
        })
        homePagerAdapter.setListener(activity as HomePagerAdapter.HomeIconsInterface)

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
                                val id = dragEvent.clipData.getItemAt(0).text.toString()
                                val widgetInfo = dragAndDropData.retrieveWidgetId(id)
                                appWidgetHost.deleteAppWidgetId(widgetInfo.getAppWidgetId())
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
        Log.d(TAG, "persistGrid()")
        var homeIconsGrid = HomeIconsGrid()
        var homeWidgetsGrid = HomeWidgetsGrid()
        val view = homePager.findViewWithTag<View>(position)
        val homeIconsTable = view.findViewById<HomeLayout>(R.id.homeIconsTable)
        for (n in 0 until homeIconsTable.childCount) {
            if (homeIconsTable.getChildAt(n) is Icon) {
                val child = homeIconsTable.getChildAt(n) as Icon
                val childParams = child.layoutParams as HomeLayout.LayoutParams
                homeIconsGrid.changeLaunchInfo(childParams.row, childParams.column, child.getLaunchInfo())
            }
            if (homeIconsTable.getChildAt(n) is WidgetContainer) {
                val child = homeIconsTable.getChildAt(n) as WidgetContainer
                val childParams = child.layoutParams as HomeLayout.LayoutParams
                homeWidgetsGrid.changeWidgetId(childParams.row, childParams.column, child.appWidgetId)
            }
        }
        var saveItJson = Json.encodeToString(homeIconsGrid)
        val editor = prefs.edit()
        editor.putString("homeIconsGrid" + position, saveItJson)
        saveItJson = Json.encodeToString(homeWidgetsGrid)
        editor.putString("homeWidgetsGrid" + position, saveItJson)
        editor.apply()
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {

        if (key != null) {
            if (key.contains("homeIconsGrid")) {
                homePagerAdapter.notifyDataSetChanged()
            }
            if (key.contains("homeWidgetsGrid")) {
                homePagerAdapter.notifyDataSetChanged()
            }
            if (key == "apps") {
                if (dock != null) {
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
        }
    }

    fun startDrag(view: View, clipData: ClipData) {
        val dsb = View.DragShadowBuilder(view)
        view.startDrag(clipData, dsb, view, 0)
    }

    fun getCurrentHomePagerItem(): Int {
        return homePager.currentItem
    }
}