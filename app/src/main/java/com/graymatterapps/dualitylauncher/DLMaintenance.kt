package com.graymatterapps.dualitylauncher

import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.graymatterapps.graymatterutils.GrayMatterUtils
import kotlinx.android.synthetic.main.activity_d_l_maintenance.*

class DLMaintenance : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    lateinit var viewAdapter: ArrayAdapter<String>
    var currentPref = "prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_d_l_maintenance)

        var prefAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.pref_types,
            R.layout.plain_spinner_item
        )
        prefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPref.adapter = prefAdapter
        spinnerPref.onItemSelectedListener = this

        viewAdapter = ArrayAdapter<String>(this, R.layout.mutiline_spinner_item)
        loadAdapter("prefs")
        preferencesView.adapter = viewAdapter
        preferencesView.setOnItemLongClickListener { adapterView, view, i, l ->
            val textView = view.findViewById<TextView>(android.R.id.text1)
            deleteWithPrompt(textView.text.toString())
            true
        }
    }

    private fun deleteWithPrompt(text: String) {
        val key = text.substringBefore(" : ", "?")
        if(key == "?") {
            GrayMatterUtils.longToast(this, "Error occurred retrieving key")
        } else {
            GrayMatterUtils.showOkCancelDialog(this,
                "Delete key ${key} ?",
            object: DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    deleteKey(key)
                }

            },
            object: DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    // Do nothing
                }
            })
        }
    }

    private fun deleteKey(key: String) {
        val editor: SharedPreferences.Editor
        try {
            if(currentPref == "prefs") {
                editor = prefs.edit()
            } else {
                editor = settingsPreferences.edit()
            }
            editor.remove(key)
            editor.apply()
        } catch (e: Exception) {
            GrayMatterUtils.longToast(this, "${e.toString()} received removing ${key}")
        }
        loadAdapter(currentPref)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        val selected = parent!!.getItemAtPosition(pos).toString()
        loadAdapter(selected)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        // Do nothing
    }

    private fun loadAdapter(pref: String) {
        currentPref = pref

        var data: Map<String, *>
        if(pref == "prefs") {
            data = prefs.all
        } else {
            data = settingsPreferences.all
        }

        viewAdapter.clear()
        data.forEach {
            viewAdapter.add(it.key + " : " + it.value.toString())
        }
        viewAdapter.notifyDataSetChanged()
    }
}