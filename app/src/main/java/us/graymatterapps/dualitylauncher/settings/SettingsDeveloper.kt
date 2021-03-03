package us.graymatterapps.dualitylauncher.settings

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import us.graymatterapps.dualitylauncher.*
import us.graymatterapps.dualitylauncher.components.widgets.WidgetDB
import us.graymatterapps.graymatterutils.GrayMatterUtils.shortToast


class SettingsDeveloper : PreferenceFragmentCompat() {

    lateinit var listener: DeveloperInterface
    val TAG = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_developer, rootKey)
        listener = generalContext as DeveloperInterface

        preferenceManager.findPreference<Preference>("update_app_list")?.setOnPreferenceClickListener {
            listener.updateAppList()
            true
        }

        preferenceManager.findPreference<Preference>("clear_icon_grid")?.setOnPreferenceClickListener {
            val okCancelDialog = AlertDialog.Builder(requireContext())
            okCancelDialog.setTitle("This option destroys data!")
            okCancelDialog.setMessage("Are you sure?")
            okCancelDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                val editor = prefs.edit()
                editor.remove("homeIconsGrid0")
                editor.remove("homeIconsGrid1")
                editor.remove("homeIconsGrid2")
                editor.remove("homeIconsGrid3")
                editor.remove("homeIconsGrid4")
                editor.putLong("notifyDataSetChanged", System.currentTimeMillis())
                editor.apply()
                shortToast(requireActivity(), "Home icon / folder grid persistence cleared...")
                dialogInterface.dismiss()
            })
            okCancelDialog.setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            okCancelDialog.show()

            true
        }

        preferenceManager.findPreference<Preference>("clear_dock")?.setOnPreferenceClickListener {
            val okCancelDialog = AlertDialog.Builder(requireContext())
            okCancelDialog.setTitle("This option destroys data!")
            okCancelDialog.setMessage("Are you sure?")
            okCancelDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                val editor = prefs.edit()
                editor.remove("dockItems")
                editor.putLong("notifyDataSetChanged", System.currentTimeMillis())
                editor.apply()
                shortToast(requireActivity(), "Dock persistence cleared...")
                dialogInterface.dismiss()
            })
            okCancelDialog.setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            okCancelDialog.show()

            true
        }

        preferenceManager.findPreference<Preference>("clear_widget_grid")?.setOnPreferenceClickListener {
            val okCancelDialog = AlertDialog.Builder(requireContext())
            okCancelDialog.setTitle("This option destroys data!")
            okCancelDialog.setMessage("Are you sure?")
            okCancelDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                val editor = prefs.edit()
                val allPrefs = prefs.all
                allPrefs.forEach{
                    if(it.key.contains("homeWidgetsGrid")){
                        editor.remove(it.key)
                    }
                    if(it.key.contains("widgetSizes")) {
                        editor.remove(it.key)
                    }
                    widgetDB = WidgetDB(appContext)
                }
                editor.putLong("notifyDataSetChanged", System.currentTimeMillis())
                editor.apply()
                listener.removeWidgets()
                listener.showWidgets()
                shortToast(
                    requireActivity(),
                    "Home widget grid persistence and AppWidgetHost data cleared..."
                )
                dialogInterface.dismiss()
            })
            okCancelDialog.setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            okCancelDialog.show()

            true
        }

        preferenceManager.findPreference<Preference>("clear_folders")?.setOnPreferenceClickListener {
            val okCancelDialog = AlertDialog.Builder(requireContext())
            okCancelDialog.setTitle("This option destroys data!")
            okCancelDialog.setMessage("Are you sure?")
            okCancelDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                val editor = prefs.edit()
                val pairs = prefs.all
                pairs.forEach {
                    if(it.key.startsWith("folder")) {
                        editor.remove(it.key)
                    }
                }
                editor.apply()
                dialogInterface.dismiss()
            })
            okCancelDialog.setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            okCancelDialog.show()


            true
        }

        preferenceManager.findPreference<Preference>("log_recents")?.setOnPreferenceClickListener {
            listener.logRecents()
            true
        }

        preferenceManager.findPreference<Preference>("icon_pack_debug")?.setOnPreferenceClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(
                R.layout.icon_pack_debug,
                null
            )
            val iconBackView = dialogView.findViewById<ImageView>(R.id.iconBackView)
            val iconMaskView = dialogView.findViewById<ImageView>(R.id.iconMaskView)
            val infoText = dialogView.findViewById<TextView>(R.id.infoText)

            if(iconPackManager.iconBack.size == 0) {
                iconBackView.setImageDrawable(
                    ContextCompat.getDrawable(
                        appContext,
                        R.drawable.ic_error
                    )
                )
            } else {
                iconBackView.setImageDrawable(iconPackManager.iconBack[0])
            }
            if(iconPackManager.iconMask == null) {
                iconMaskView.setImageDrawable(
                    ContextCompat.getDrawable(
                        appContext,
                        R.drawable.ic_error
                    )
                )
            } else {
                iconMaskView.setImageDrawable(iconPackManager.iconMask)
            }
            infoText.text = "Scale: ${iconPackManager.scale}\nSize: ${iconPackManager.iconSize}"
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(settingsPreferences.getString("choose_icon_pack", "Default"))
            builder.setView(dialogView)
            builder.setPositiveButton("Ok") { dialog, which ->
                dialog.dismiss()
            }
            builder.show()
            true
        }

        preferenceManager.findPreference<Preference>("restore_folder")?.setOnPreferenceClickListener {
            val dialog = AlertDialog.Builder(requireContext())
            dialog.setTitle("Enter folder unique ID")
            val input = EditText(requireContext())
            input.inputType = InputType.TYPE_CLASS_NUMBER
            dialog.setView(input)
            dialog.setPositiveButton("Ok", DialogInterface.OnClickListener { dialogInterface, i ->
                val id = input.text.toString()
                var info = LaunchInfo()
                info.setType(LaunchInfo.FOLDER)
                info.setFolderName("Restored Folder")
                info.setFolderUniqueId(id.toLong())
                replicator.addFolderAll(info, 0, 0, 0)
                dialogInterface.dismiss()
            })
            dialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            dialog.show()
            true
        }
    }

    interface DeveloperInterface {
        fun updateAppList()
        fun logRecents()
        fun removeWidgets(leavePager: Boolean = false)
        fun showWidgets()
    }
}