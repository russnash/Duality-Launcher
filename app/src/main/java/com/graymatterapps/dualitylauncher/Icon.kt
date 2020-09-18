package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils

class Icon(
    private val parentActivity: MainActivity,
    attrs: AttributeSet?,
    activityInfo: String,
    packageInfo: String,
    userSerial: Long,
    isDragTarget: Boolean,
    isBlankOnDrag: Boolean
) : LinearLayout(parentActivity, attrs) {

    constructor(con: MainActivity, attrs: AttributeSet?) : this(con, attrs, "", "", 0, true, false)
    constructor(
        con: MainActivity,
        attrs: AttributeSet?,
        activityInfo: String,
        packageInfo: String,
        userSerial: Long
    ) : this(con, attrs, activityInfo, packageInfo, userSerial, true, false)

    private lateinit var listener: IconInterface
    lateinit var parentLayout: HomeLayout
    var iconLayout: LinearLayout
    var icon: ImageView
    var label: TextView
    private var launchInfo = LaunchInfo()
    private val displayId: Int
    private var dragTarget = true
    private var blankOnDrag = false
    private var isDockIcon = false
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 20)
    private val pulseAnim = AnimationUtils.loadAnimation(context, R.anim.pulse_alpha)
    val TAG = javaClass.simpleName

    init {
        inflate(context, R.layout.icon, this)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayId = wm.defaultDisplay.displayId

        dragTarget = isDragTarget

        if (attrs != null) {
            setupAttrs(attrs)
        }

        iconLayout = findViewById(R.id.iconLayout)
        icon = findViewById(R.id.iconImage)
        label = findViewById(R.id.label)

        blankOnDrag = isBlankOnDrag

        launchInfo.setActivityName(activityInfo)
        launchInfo.setPackageName(packageInfo)
        launchInfo.setUserSerial(userSerial)

        setupIcon()

        if (dragTarget) {
            icon.setOnDragListener { view, dragEvent ->
                if (dragEvent != null) {

                    var respondToDrag = false
                    try {
                        if (dragEvent.clipDescription.label.toString().equals("launchInfo")) {
                            respondToDrag = true
                        }
                        if (dragEvent.clipDescription.label.toString().equals("widget")) {
                            // Dock icons don't respond to widget drags!
                            respondToDrag = !isDockIcon
                        }
                        if (isFolderOpen) {
                            respondToDrag = false
                        }
                    } catch (e: Exception) {
                        respondToDrag = false
                    }

                    when (dragEvent.action) {
                        DragEvent.ACTION_DRAG_STARTED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundResource(R.drawable.icon_drag_target)
                                //iconLayout.startAnimation(pulseAnim)
                            }
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundColor(enteredColor)
                            }
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundResource(R.drawable.icon_drag_target)
                                //iconLayout.startAnimation(pulseAnim)
                            }
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            iconLayout.setBackgroundColor(Color.TRANSPARENT)
                            //iconLayout.clearAnimation()
                        }
                        DragEvent.ACTION_DROP -> {
                            if (respondToDrag) {
                                if (dragEvent.clipDescription.label.toString().equals("launchInfo")
                                ) {
                                    if (launchInfo.getActivityName() != "") {
                                        val id = dragEvent.clipData.getItemAt(0).text.toString()
                                        val info = dragAndDropData.retrieveLaunchInfo(id)
                                        convertToFolder(info)
                                    } else {
                                        val id = dragEvent.clipData.getItemAt(0).text.toString()
                                        launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                                    }
                                    listener.onIconChanged()
                                }
                                if (dragEvent.clipDescription.label.toString().equals("widget")) {
                                    val id = dragEvent.clipData.getItemAt(0).text.toString()
                                    val widgetInfo = dragAndDropData.retrieveWidgetId(id)
                                    convertToWidget(widgetInfo)
                                    listener.onIconChanged()
                                }
                            }
                        }
                    }
                }
                true
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this.parent is HomeLayout) {
            parentLayout = this.parent as HomeLayout
        }
    }

    private fun setupAttrs(attrs: AttributeSet) {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.Icon, 0, 0)
        dragTarget = array.getBoolean(R.styleable.Icon_dragTarget, true)
        blankOnDrag = array.getBoolean(R.styleable.Icon_blankOnDrag, false)
    }

    fun setupIcon() {
        if (launchInfo.getActivityName() != "") {
            icon.setImageDrawable(appList.getIcon(launchInfo))
            label.text = appList.getLabel(launchInfo)

            icon.setOnClickListener {
                listener.onLaunch(launchInfo, displayId)
            }

            icon.setOnLongClickListener { view ->
                val id = System.currentTimeMillis().toString()
                val passedLaunchInfo = launchInfo.copy()
                dragAndDropData.addLaunchInfo(passedLaunchInfo, id)
                var clipData = ClipData.newPlainText("launchInfo", id)

                listener.onDragStarted(this, clipData)

                if (blankOnDrag) {
                    launchInfo.setActivityName("")
                    launchInfo.setPackageName("")
                    launchInfo.setUserSerial(0)
                    icon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
                    label.text = ""
                }
                listener.onIconChanged()
                true
            }
        } else {
            icon.setOnLongClickListener { view ->
                listener.onLongClick(view)
                true
            }
            label.setOnLongClickListener { view ->
                listener.onLongClick(view)
                true
            }
            icon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            label.text = ""
        }
    }

    fun setListener(iconInterface: IconInterface) {
        listener = iconInterface
    }

    fun setLaunchInfo(appActivityName: String, packageName: String, userSerial: Long) {
        launchInfo.setActivityName(appActivityName)
        launchInfo.setPackageName(packageName)
        launchInfo.setUserSerial(userSerial)
        setupIcon()
    }

    fun setLaunchInfo(launchInfos: LaunchInfo) {
        launchInfo = launchInfos
        setupIcon()
    }

    fun getLaunchInfo(): LaunchInfo {
        return launchInfo
    }

    fun setBlankOnDrag(state: Boolean) {
        blankOnDrag = state
    }

    fun setDockIcon(state: Boolean) {
        isDockIcon = state
    }

    fun convertToFolder(info: LaunchInfo) {
        var folder: Folder
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (info.getType() == LaunchInfo.ICON) {
            folder = Folder(parentActivity, null, parentActivity.getString(R.string.new_folder))
            folder.addFolderApp(launchInfo)
            folder.addFolderApp(info)
        } else {
            folder = Folder(parentActivity, null, info.getFolderName(), info)
        }
        folder.layoutParams = params
        folder.setListener(parentActivity.homePagerAdapter as Folder.FolderInterface)
        parentLayout.addView(folder)
        parentLayout.removeView(this)
    }

    fun convertToWidget(widgetInfo: WidgetInfo) {
        val widgetContainer = WidgetContainer(
            parentActivity,
            widgetInfo.getAppWidgetId(),
            widgetInfo.getAppWidgetProviderInfo()
        )
        val params = this.layoutParams as HomeLayout.LayoutParams
        widgetContainer.layoutParams = params
        widgetContainer.setListener(parentActivity as WidgetContainer.WidgetInterface)
        parentLayout.addView(widgetContainer)
        parentLayout.removeView(this)
    }

    interface IconInterface {
        fun onIconChanged()
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onLongClick(view: View)
    }
}