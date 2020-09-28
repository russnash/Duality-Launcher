package com.graymatterapps.dualitylauncher

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils
import kotlinx.android.synthetic.main.fragment_drawer.*
import kotlinx.android.synthetic.main.icon.view.*

class DrawerFragment() : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var gridLayoutManager: GridLayoutManager
    lateinit var adapter: AppDrawerAdapter
    lateinit var parent: MainActivity
    val TAG = javaClass.simpleName

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        settingsPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_drawer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parent = activity as MainActivity
        gridLayoutManager = if(GrayMatterUtils.isLandscape(parent)){
            GridLayoutManager(context?.applicationContext, 12)
        } else {
            GridLayoutManager(context?.applicationContext, 6)
        }

        drawer.layoutManager = gridLayoutManager
        adapter = AppDrawerAdapter(parent, appList.apps)
        drawer.adapter = adapter
        adapter.filterWork(false)
        buttonApps.alpha = 1.0f
        buttonWork.alpha = 0.5f

        buttonApps.setOnClickListener {
            adapter.filterWork(false)
            buttonApps.alpha = 1.0f
            buttonWork.alpha = 0.5f
            notifyDataSetChanged()
        }

        buttonWork.setOnClickListener {
            adapter.filterWork(true)
            buttonApps.alpha = 0.5f
            buttonWork.alpha = 1.0f
            notifyDataSetChanged()
        }

        setDrawerBackground()
        notifyDataSetChanged()

        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        settingsPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    private fun setDrawerBackground() {
        var color = settingsPreferences.getInt(
            "app_drawer_background",
            -16777216
        )
        drawerLayout.setBackgroundColor(color)
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key == "apps") {
            notifyDataSetChanged()
        }

        if (key == "app_drawer_background") {
            setDrawerBackground()
        }

        if (key == "app_drawer_text") {
            changeTextColors()
        }
    }

    fun notifyDataSetChanged() {
        adapter.filterWork(adapter.filteredWork)
        adapter.notifyDataSetChanged()
    }

    fun changeTextColors() {
        val color = settingsPreferences.getInt("app_drawer_text", Color.WHITE)

        var x: Int = drawer.childCount
        var i = 0
        while (i < x) {
            val holder: RecyclerView.ViewHolder = drawer.getChildViewHolder(drawer.getChildAt(i))
            val text = holder.itemView.label
            text.setTextColor(color)
            ++i
        }
    }
}