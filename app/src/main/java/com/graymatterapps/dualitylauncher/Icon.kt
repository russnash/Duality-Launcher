package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.graphics.ColorUtils

class Icon(
    private val parentActivity: MainActivity,
    attrs: AttributeSet?,
    activityInfo: String,
    packageInfo: String,
    userSerial: Long,
    isDragTarget: Boolean,
    isBlankOnDrag: Boolean,
    var replicate: Boolean = false,
    var page: Int
) : LinearLayout(parentActivity, attrs) {

    constructor(con: MainActivity, attrs: AttributeSet?, replicate: Boolean, page: Int) : this(con, attrs, "", "", 0, true, false, replicate, page)
    constructor(
        con: MainActivity,
        attrs: AttributeSet?,
        activityInfo: String,
        packageInfo: String,
        userSerial: Long
    ) : this(con, attrs, activityInfo, packageInfo, userSerial, true, false, false, 0)

    private lateinit var listener: IconInterface
    lateinit var parentLayout: HomeLayout
    var iconLayout: LinearLayout
    var icon: ImageView
    var label: TextView
    private var launchInfo = LaunchInfo()
    private var dragTarget = true
    private var blankOnDrag = false
    private var isDockIcon = false
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 20)
    private val pulseAnim = AnimationUtils.loadAnimation(context, R.anim.pulse_alpha)
    val TAG = javaClass.simpleName

    init {
        inflate(context, R.layout.icon, this)

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
                    } catch (e: Exception) {
                        respondToDrag = false
                    }

                    when (dragEvent.action) {
                        DragEvent.ACTION_DRAG_STARTED -> {
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundResource(R.drawable.icon_drag_target)
                            }
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            if (respondToDrag) {
                                iconLayout.setBackgroundColor(Color.TRANSPARENT)
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
                                    if(!isDockIcon) {
                                        if (launchInfo.getActivityName() != "") {
                                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                                            val info = dragAndDropData.retrieveLaunchInfo(id)
                                            convertToFolder(info)
                                        } else {
                                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                                            launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                                            if (launchInfo.getType() == LaunchInfo.ICON) {
                                                if (this.parent is HomeLayout) {
                                                    replicate = false
                                                    val params =
                                                        this.layoutParams as HomeLayout.LayoutParams
                                                    replicator.changeIcon(
                                                        parentActivity.displayId,
                                                        launchInfo,
                                                        page,
                                                        params.row,
                                                        params.column
                                                    )
                                                }
                                                setupIcon()
                                            } else {
                                                convertToFolder(launchInfo)
                                            }
                                        }
                                    } else {
                                        val id = dragEvent.clipData.getItemAt(0).text.toString()
                                        launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                                        if(launchInfo.getType() == LaunchInfo.ICON) {
                                            this.launchInfo = launchInfo
                                            setupIcon()
                                        }
                                    }
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
            if(replicate) {
                val params = this.layoutParams as HomeLayout.LayoutParams
                replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
                replicator.addIcon(parentActivity.displayId, launchInfo, page, params.row, params.column)
            }
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
                listener.onLaunch(launchInfo, parentActivity.displayId)
            }

            if(replicate) {
                val params = this.layoutParams as HomeLayout.LayoutParams
                replicator.addIcon(
                    parentActivity.displayId,
                    launchInfo,
                    page,
                    params.row,
                    params.column
                )
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
                    if(this.parent is HomeLayout) {
                        val params = this.layoutParams as HomeLayout.LayoutParams
                        replicator.changeIcon(
                            parentActivity.displayId,
                            launchInfo,
                            page,
                            params.row,
                            params.column
                        )
                    }
                } else {
                    if(this.parent is HomeLayout) {
                        val params = this.layoutParams as HomeLayout.LayoutParams
                        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
                        parentLayout.removeView(this)
                    }
                }
                if(isDockIcon){
                    parentActivity.dock.persistDock()
                }
                true
            }
        }
        if(isDockIcon){
            parentActivity.dock.persistDock()
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
            folder = Folder(parentActivity, null, parentActivity.getString(R.string.new_folder), null, true, page)
            folder.addFolderApp(launchInfo)
            folder.addFolderApp(info)
        } else {
            folder = Folder(parentActivity, null, info.getFolderName(), info, true, page)
        }
        folder.layoutParams = params
        folder.setListener(parentActivity.homePagerAdapter as Folder.FolderInterface)
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.addView(folder)
        parentLayout.removeView(this)
    }

    /*
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
        replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
        parentLayout.removeView(this)
    }

     */

    interface IconInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onLongClick(view: View)
        fun resetResize()
    }
}