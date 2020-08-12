package com.graymatterapps.dualitylauncher

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.dualitylauncher.MainActivity.Companion.appList
import kotlinx.android.synthetic.main.icon.view.*

class DrawerFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var gridLayoutManager: GridLayoutManager
    lateinit var adapter: AppDrawerAdapter
    lateinit var drawer: RecyclerView
    lateinit var drawerLayout: LinearLayout
    lateinit var buttonApps: Button
    lateinit var buttonWork: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)

        val view = inflater.inflate(R.layout.fragment_drawer, container, false)

        drawer = view.findViewById(R.id.drawer)
        gridLayoutManager = GridLayoutManager(context?.applicationContext, 6)
        drawer.layoutManager = gridLayoutManager
        adapter = AppDrawerAdapter(requireContext(), appList.apps)
        drawer.adapter = adapter
        adapter.filterWork(false)

        drawerLayout = view.findViewById(R.id.drawerLayout)

        buttonApps = view.findViewById(R.id.buttonApps)
        buttonWork = view.findViewById(R.id.buttonWork)
        buttonApps.alpha = 1.0f
        buttonWork.alpha = 0.5f
        buttonApps.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                adapter.filterWork(false)
                buttonApps.alpha = 1.0f
                buttonWork.alpha = 0.5f
                notifyDataSetChanged()
            }

        })

        buttonWork.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                adapter.filterWork(true)
                buttonApps.alpha = 0.5f
                buttonWork.alpha = 1.0f
                notifyDataSetChanged()
            }
        })

        setDrawerBackground()
        notifyDataSetChanged()

        return view
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        settingsPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    private fun setDrawerBackground(){
        var basicColor = MainActivity.colorPrefToColor(settingsPreferences.getString("app_drawer_background", "Black"))
        var alpha = settingsPreferences.getInt("app_drawer_background_alpha", 80)
        var color = ColorUtils.setAlphaComponent(basicColor, alpha)
        drawerLayout.setBackgroundColor(color)
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if(key == "apps"){
            notifyDataSetChanged()
        }

        if(key == "app_drawer_background"){
            setDrawerBackground()
        }

        if(key == "app_drawer_background_alpha"){
            setDrawerBackground()
        }

        if(key == "app_drawer_text"){
            changeTextColors()
        }
    }

    fun notifyDataSetChanged(){
        adapter.notifyDataSetChanged()
    }

    fun changeTextColors() {
        val color = MainActivity.colorPrefToColor(settingsPreferences.getString("app_drawer_text", "White"))

        var x: Int = drawer.childCount
        var i = 0
        while (i < x) {
            val holder: RecyclerView.ViewHolder = drawer.getChildViewHolder(drawer.getChildAt(i))
            val text = holder.itemView.label
            text.setTextColor(color)
            ++i
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): DrawerFragment {
            return DrawerFragment()
        }
    }
}