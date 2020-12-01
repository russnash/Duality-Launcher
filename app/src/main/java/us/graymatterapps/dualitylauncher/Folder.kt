package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Folder(
    private val parentActivity: MainActivity,
    attrs: AttributeSet?,
    name: String,
    info: LaunchInfo? = null,
    var replicate: Boolean = false,
    var page: Int
) : LinearLayout(parentActivity, attrs), SharedPreferences.OnSharedPreferenceChangeListener {

    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 20)
    private val folderLayout: LinearLayout
    private val folderIcon: ImageView
    private val folderLabel: TextView

    @Serializable
    private var folderApps = ArrayList<LaunchInfo>()
    private var launchInfo: LaunchInfo = LaunchInfo()
    private lateinit var listener: FolderInterface
    lateinit var parentLayout: HomeLayout
    private val pulseAnim = AnimationUtils.loadAnimation(context, R.anim.pulse_alpha)

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
        settingsPreferences.registerOnSharedPreferenceChangeListener(this)

        inflate(parentActivity, R.layout.folder, this)
        folderLayout = findViewById(R.id.folderLayout)
        folderIcon = findViewById(R.id.folderImage)
        folderLabel = findViewById(R.id.folderLabel)

        folderIcon.setImageResource(R.drawable.ic_folder)
        folderLabel.text = name

        if (info == null) {
            launchInfo.setType(LaunchInfo.FOLDER)
            launchInfo.setFolderUniqueId(System.currentTimeMillis())
            folderLabel.text = parentActivity.getString(R.string.new_folder)
            launchInfo.setFolderName(parentActivity.getString(R.string.new_folder))
        } else {
            launchInfo = info
            folderLabel.text = launchInfo.getFolderName()
            depersistFolderApps()
            folderIcon.setImageBitmap(makeFolderIcon())
        }

        setupDragListener()
        folderIcon.setOnClickListener {
            showFolder()
        }

        folderIcon.setOnLongClickListener {
            val id = System.currentTimeMillis().toString()
            val passedLaunchInfo = launchInfo.copy()
            dragAndDropData.addLaunchInfo(passedLaunchInfo, id)
            var clipData = ClipData.newPlainText("launchInfo", id)
            listener.onDragStarted(this, clipData)
            convertToEmpty()
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this.parent is HomeLayout) {
            parentLayout = this.parent as HomeLayout
            if (replicate) {
                val params = this.layoutParams as HomeLayout.LayoutParams
                replicator.addFolder(
                    parentActivity.displayId,
                    launchInfo,
                    page,
                    params.row,
                    params.column
                )
            }
        }
    }

    fun verifyApps() {
        appList.lock.lock()
        val tempApps = ArrayList<LaunchInfo>()
        folderApps.forEach {
            if (it.getType() == LaunchInfo.ICON) {
                if (appList.isAppInstalled(it)) {
                    tempApps.add(it)
                }
            } else {
                tempApps.add(it)
            }
        }
        appList.lock.unlock()
        folderApps = tempApps
        persistFolderApps()
        makeFolderIcon()
    }

    private fun setupDragListener() {
        folderIcon.setOnDragListener { view, dragEvent ->
            if (dragEvent != null) {
                var respondToDrag = false
                try {
                    if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                        respondToDrag = true
                    }
                    if (dragEvent.clipDescription.label.toString().equals("widget")) {
                        // Folders don't respond to widget drags to avoid tears!
                        respondToDrag = false
                    }
                } catch (e: Exception) {
                    respondToDrag = false
                }

                when (dragEvent.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (respondToDrag) {
                            folderLayout.setBackgroundResource(R.drawable.icon_drag_target)
                        }
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        if (respondToDrag) {
                            folderLayout.setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        folderLayout.setBackgroundColor(Color.TRANSPARENT)
                        //folderLayout.clearAnimation()
                    }
                    DragEvent.ACTION_DROP -> {
                        if (respondToDrag) {
                            if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                                val id = dragEvent.clipData.getItemAt(0).text.toString()
                                val info = dragAndDropData.retrieveLaunchInfo(id)
                                if (info.getType() == LaunchInfo.ICON) {
                                    addFolderItem(info)
                                }
                                if (info.getType() == LaunchInfo.DUALLAUNCH) {
                                    addFolderItem(info)
                                }
                            }
                        }
                    }
                }
            }
            true
        }
    }

    private fun makeFolderIcon(): Bitmap {
        val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val drawable: Drawable =
            ContextCompat.getDrawable(parentActivity, R.drawable.ic_launcher_background)!!
        var bitmap = drawableToBitmap(drawable)
        var canvas = Canvas(bitmap!!)
        canvas.drawRect(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat(), clearPaint)

        // Background
        var backDrawable: Drawable? = null
        var skip: Boolean = false
        when (settingsPreferences.getString("folder_icon_background", "None")) {
            "Square" -> backDrawable =
                ContextCompat.getDrawable(parentActivity, R.drawable.square_background)
            "Rounded Square" -> backDrawable =
                ContextCompat.getDrawable(parentActivity, R.drawable.rounded_square_background)
            "Circle" -> backDrawable =
                ContextCompat.getDrawable(parentActivity, R.drawable.circle_background)
            "Shelf" -> backDrawable =
                ContextCompat.getDrawable(parentActivity, R.drawable.shelf_background)
            else -> skip = true
        }
        if (!skip) {
            if (backDrawable != null) {
                backDrawable.setTint(
                    settingsPreferences.getInt(
                        "folder_icon_background_color",
                        Color.BLACK
                    )
                )
                var backBitmap = Bitmap.createBitmap(
                    backDrawable.intrinsicWidth,
                    backDrawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                backDrawable.setBounds(0, 0, canvas.width, canvas.height)
                backDrawable.draw(canvas)
            }
        }

        if (settingsPreferences.getString(
                "folder_icon_preview",
                "First and Last"
            ) == "First and Last"
        ) {
            // First icon
            var firstBitmap =
                ContextCompat.getDrawable(parentActivity, R.drawable.ic_folder)!!.toBitmap()
            if (folderApps.size != 0) {
                if(folderApps[0].getType() == LaunchInfo.ICON) {
                    firstBitmap = appList.getIcon(folderApps[0]).toBitmap()
                } else {
                    val dl = DualLaunch(parentActivity, null, folderApps[0].getDualLaunchName(), folderApps[0], false, 0)
                    firstBitmap = dl.makeDualLaunchIcon()
                }
            }
            var srcRect: Rect = Rect(0, 0, firstBitmap.width, firstBitmap.height)
            var dstRect: Rect = Rect(
                0, 0, (canvas.width * 0.66).toInt(),
                (canvas.height * 0.66).toInt()
            )
            canvas.drawBitmap(firstBitmap, srcRect, dstRect, null)

            // Last icon
            var secondBitmap =
                ContextCompat.getDrawable(parentActivity, R.drawable.ic_folder)!!.toBitmap()
            if (folderApps.size != 0) {
                if(folderApps[folderApps.size -1].getType() == LaunchInfo.ICON) {
                    secondBitmap = appList.getIcon(folderApps[folderApps.size -1]).toBitmap()
                } else {
                    val dl = DualLaunch(parentActivity, null, folderApps[folderApps.size -1].getDualLaunchName(), folderApps[folderApps.size -1], false, 0)
                    secondBitmap = dl.makeDualLaunchIcon()
                }
            }
            srcRect = Rect(0, 0, secondBitmap.width, secondBitmap.height)
            dstRect = Rect(canvas.width / 3, canvas.height / 3, canvas.width, canvas.height)
            canvas.drawBitmap(secondBitmap, srcRect, dstRect, null)
        } else {
            // First icon
            var firstBitmap =
                ContextCompat.getDrawable(parentActivity, R.drawable.ic_folder)!!.toBitmap()
            if (folderApps.size != 0) {
                if(folderApps[0].getType() == LaunchInfo.ICON) {
                    firstBitmap = appList.getIcon(folderApps[0]).toBitmap()
                } else {
                    val dl = DualLaunch(parentActivity, null, folderApps[0].getDualLaunchName(), folderApps[0], false, 0)
                    firstBitmap = dl.makeDualLaunchIcon()
                }
            }
            var srcRect: Rect = Rect(0, 0, firstBitmap.width, firstBitmap.height)
            var dstRect: Rect = Rect(
                0,
                0,
                (canvas.width * 0.5).toInt(),
                (canvas.height * 0.5).toInt()
            )
            canvas.drawBitmap(firstBitmap, srcRect, dstRect, null)

            // Second icon
            var secondBitmap =
                ContextCompat.getDrawable(parentActivity, R.drawable.ic_folder)!!.toBitmap()
            if (folderApps.size >= 2) {
                if(folderApps[1].getType() == LaunchInfo.ICON) {
                    secondBitmap = appList.getIcon(folderApps[1]).toBitmap()
                } else {
                    val dl = DualLaunch(parentActivity, null, folderApps[1].getDualLaunchName(), folderApps[1], false, 0)
                    secondBitmap = dl.makeDualLaunchIcon()
                }
            }
            srcRect = Rect(0, 0, secondBitmap.width, secondBitmap.height)
            dstRect = Rect(
                (canvas.width * 0.5).toInt(),
                0,
                canvas.width,
                (canvas.height * 0.5).toInt()
            )
            canvas.drawBitmap(secondBitmap, srcRect, dstRect, null)

            // Third icon
            var thirdBitmap =
                ContextCompat.getDrawable(parentActivity, R.drawable.ic_folder)!!.toBitmap()
            if (folderApps.size >= 3) {
                if(folderApps[2].getType() == LaunchInfo.ICON) {
                    thirdBitmap = appList.getIcon(folderApps[2]).toBitmap()
                } else {
                    val dl = DualLaunch(parentActivity, null, folderApps[2].getDualLaunchName(), folderApps[2], false, 0)
                    thirdBitmap = dl.makeDualLaunchIcon()
                }
            }
            srcRect = Rect(0, 0, thirdBitmap.width, thirdBitmap.height)
            dstRect = Rect(
                0,
                (canvas.height * 0.5).toInt(),
                (canvas.width * 0.5).toInt(),
                canvas.height
            )
            canvas.drawBitmap(thirdBitmap, srcRect, dstRect, null)

            // Fourth icon
            var fourthBitmap =
                ContextCompat.getDrawable(parentActivity, R.drawable.ic_folder)!!.toBitmap()
            if (folderApps.size >= 4) {
                if(folderApps[3].getType() == LaunchInfo.ICON) {
                    fourthBitmap = appList.getIcon(folderApps[3]).toBitmap()
                } else {
                    val dl = DualLaunch(parentActivity, null, folderApps[3].getDualLaunchName(), folderApps[3], false, 0)
                    fourthBitmap = dl.makeDualLaunchIcon()
                }
            }
            srcRect = Rect(0, 0, fourthBitmap.width, fourthBitmap.height)
            dstRect = Rect(
                (canvas.width * 0.5).toInt(),
                (canvas.height * 0.5).toInt(),
                canvas.width,
                canvas.height
            )
            canvas.drawBitmap(fourthBitmap, srcRect, dstRect, null)
        }

        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun depersistFolderApps() {
        var tempFolderApps = ArrayList<LaunchInfo>()
        val loadItJson = prefs.getString("folder" + launchInfo.getFolderUniqueId(), "")
        if (loadItJson != "") {
            tempFolderApps = loadItJson?.let { Json.decodeFromString(it) }!!
        }
        folderApps.clear()
        folderApps.addAll(tempFolderApps)
        folderIcon.setImageBitmap(makeFolderIcon())
    }

    private fun updateIcon() {
        folderIcon.setImageBitmap(makeFolderIcon())
    }

    fun persistFolderApps() {
        val saveItJson = Json.encodeToString(folderApps)
        val editor = prefs.edit()
        editor.putString("folder" + launchInfo.getFolderUniqueId(), saveItJson)
        editor.apply()
        folderIcon.setImageBitmap(makeFolderIcon())
    }

    fun setFolderName(name: String) {
        folderLabel.text = name
        launchInfo.setFolderName(name)
        val params = this.layoutParams as HomeLayout.LayoutParams
        replicator.changeFolder(
            parentActivity.displayId,
            launchInfo,
            page,
            params.row,
            params.column
        )
    }

    fun setLaunchInfo(info: LaunchInfo) {
        launchInfo = info
        folderLabel.text = info.getFolderName()
    }

    fun getLaunchInfo(): LaunchInfo {
        return launchInfo
    }

    fun addFolderItem(info: LaunchInfo) {
        if (!folderApps.contains(info)) {
            folderApps.add(info)
            sortFolder()
            persistFolderApps()
        }
    }

    fun sortFolder() {
        if (settingsPreferences.getBoolean("sort_folders", true)) {
            folderApps.sortBy {
                if(it.getType() == LaunchInfo.DUALLAUNCH) {
                    it.getDualLaunchName().toLowerCase()
                } else {
                    appList.getLabel(it).toLowerCase()
                }
            }
        }
    }

    fun addFolderItemAtPosition(addInfo: LaunchInfo, posInfo: LaunchInfo) {
        var pos = folderApps.indexOf(posInfo)
        folderApps.add(pos, addInfo)
        sortFolder()
        persistFolderApps()
    }

    fun removeFolderApp(info: LaunchInfo) {
        folderApps.remove(info)
        sortFolder()
        persistFolderApps()
        folderIcon.setImageBitmap(makeFolderIcon())
    }

    private fun showFolder() {
        verifyApps()
        listener.onShowFolder(true)
        listener.onSetupFolder(folderApps, SpannableStringBuilder(folderLabel.text), this)
    }

    fun setListener(ear: FolderInterface) {
        listener = ear
    }

    fun convertToEmpty() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.removeView(this)
        parentActivity.persistGrid(page)
    }

    interface FolderInterface {
        fun onShowFolder(state: Boolean)
        fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder)
        fun onDragStarted(view: View, clipData: ClipData)
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key != null) {
            if (key == "folder" + launchInfo.getFolderUniqueId()) {
                depersistFolderApps()
            }
            if (key == "folder_icon_background") {
                updateIcon()
            }
            if (key == "folder_icon_background_color") {
                updateIcon()
            }
            if (key == "folder_icon_preview") {
                updateIcon()
            }
            if (key == "sort_folders") {
                sortFolder()
                persistFolderApps()
            }
        }
    }
}