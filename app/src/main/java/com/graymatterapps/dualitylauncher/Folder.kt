package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Folder(
    private val con: Context,
    attrs: AttributeSet?,
    name: String,
    info: LaunchInfo? = null
) : LinearLayout(con, attrs), SharedPreferences.OnSharedPreferenceChangeListener {

    private val displayId: Int
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
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayId = wm.defaultDisplay.displayId

        prefs.registerOnSharedPreferenceChangeListener(this)

        inflate(con, R.layout.folder, this)
        folderLayout = findViewById(R.id.folderLayout)
        folderIcon = findViewById(R.id.folderImage)
        folderLabel = findViewById(R.id.folderLabel)

        folderIcon.setImageResource(R.drawable.ic_folder)
        folderLabel.text = name

        if (info == null) {
            launchInfo.setType(LaunchInfo.FOLDER)
            launchInfo.setFolderUniqueId(System.currentTimeMillis())
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
            convertToIcon()
            listener.onFolderChanged()
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this.parent is HomeLayout) {
            parentLayout = this.parent as HomeLayout
        }
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
                        if (respondToDrag) {
                            folderLayout.setBackgroundResource(R.drawable.icon_drag_target)
                            //folderLayout.startAnimation(pulseAnim)
                        }
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (respondToDrag) {
                            folderLayout.setBackgroundColor(enteredColor)
                        }
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        if (respondToDrag) {
                            folderLayout.setBackgroundResource(R.drawable.icon_drag_target)
                            //folderLayout.startAnimation(pulseAnim)
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
                                val info = MainActivity.dragAndDropData.retrieveLaunchInfo(id)
                                addFolderApp(info)
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
            mainContext.resources.getDrawable(R.drawable.ic_launcher_background)
        var bitmap = drawableToBitmap(drawable)
        var canvas = Canvas(bitmap!!)
        canvas.drawRect(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat(), clearPaint)

        var firstBitmap = mainContext.resources.getDrawable(R.drawable.ic_folder).toBitmap()
        if (folderApps.size != 0) {
            firstBitmap = appList.getIcon(folderApps[0]).toBitmap()
        }
        var srcRect: Rect = Rect(0, 0, firstBitmap.width, firstBitmap.height)
        var dstRect: Rect = Rect(
            0, 0, (canvas.width * 0.66).toInt(),
            (canvas.height * 0.66).toInt()
        )
        canvas.drawBitmap(firstBitmap, srcRect, dstRect, null)

        var secondBitmap = mainContext.resources.getDrawable(R.drawable.ic_folder).toBitmap()
        if (folderApps.size != 0) {
            secondBitmap = appList.getIcon(folderApps[folderApps.size - 1]).toBitmap()
        }
        srcRect = Rect(0, 0, secondBitmap.width, secondBitmap.height)
        dstRect = Rect(canvas.width / 3, canvas.height / 3, canvas.width, canvas.height)
        canvas.drawBitmap(secondBitmap, srcRect, dstRect, null)

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
    }

    private fun persistFolderApps() {
        val saveItJson = Json.encodeToString(folderApps)
        val editor = prefs.edit()
        editor.putString("folder" + launchInfo.getFolderUniqueId(), saveItJson)
        editor.apply()
    }

    fun setFolderName(name: String) {
        folderLabel.text = name
        launchInfo.setFolderName(name)
        listener.onFolderChanged()
    }

    fun getLaunchInfo(): LaunchInfo {
        return launchInfo
    }

    fun addFolderApp(info: LaunchInfo) {
        if (!folderApps.contains(info)) {
            folderApps.add(info)
            folderApps.sortBy { appList.getLabel(it) }
            persistFolderApps()
            folderIcon.setImageBitmap(makeFolderIcon())
        }
    }

    fun removeFolderApp(info: LaunchInfo) {
        folderApps.remove(info)
        folderApps.sortBy { appList.getLabel(it) }
        persistFolderApps()
        folderIcon.setImageBitmap(makeFolderIcon())
    }

    private fun showFolder() {
        listener.onShowFolder(true)
        listener.onSetupFolder(folderApps, SpannableStringBuilder(folderLabel.text), this)
    }

    fun setListener(ear: FolderInterface) {
        listener = ear
    }

    fun convertToIcon() {
        val icon = Icon(mainContext, null)
        val params = this.layoutParams as HomeLayout.LayoutParams
        icon.setLaunchInfo(LaunchInfo())
        icon.layoutParams = params
        icon.setListener(listener as Icon.IconInterface)
        parentLayout.addView(icon)
        parentLayout.removeView(this)
    }

    interface FolderInterface {
        fun onShowFolder(state: Boolean)
        fun onSetupFolder(apps: ArrayList<LaunchInfo>, name: Editable, folder: Folder)
        fun onFolderChanged()
        fun onDragStarted(view: View, clipData: ClipData)
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key != null) {
            if (key == "folder" + launchInfo.getFolderUniqueId()) {
                depersistFolderApps()
            }
        }
    }
}