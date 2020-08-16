package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.graymatterapps.dualitylauncher.MainActivity.Companion.appList
import com.graymatterapps.dualitylauncher.MainActivity.Companion.dragAndDropData

class Icon(private val con: Context,
           attrs: AttributeSet?,
           activityInfo: String,
           packageInfo: String,
           userSerial: Long,
           isDragTarget: Boolean,
           isBlankOnDrag: Boolean): LinearLayout(con, attrs) {

    constructor(con: Context, attrs: AttributeSet?) : this(con, attrs, "", "",0, true, false)
    constructor(con: Context, attrs: AttributeSet?, activityInfo: String, packageInfo: String, userSerial: Long) : this(con, attrs, activityInfo, packageInfo, userSerial, true, false)

    private lateinit var listener: IconInterface
    var iconLayout: LinearLayout
    var icon: ImageView
    var label: TextView
    var launchInfo = LaunchInfo()
    private val displayId: Int
    private var dragTarget = true
    private var blankOnDrag = false
    private val enteredColor = ColorUtils.setAlphaComponent(Color.GREEN, 80)

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

        if(dragTarget){
            icon.setOnDragListener { view, dragEvent ->
                if (dragEvent != null) {

                    // Check the clip description label and only respond to drag events if its
                    // a "launchInfo" drag (another Icon)
                    var respondToDrag = false
                    try{
                        if(dragEvent.clipDescription.label.toString().equals("launchInfo")){
                            respondToDrag = true
                        }
                    } catch (e: Exception) {
                        respondToDrag = false
                    }

                    when (dragEvent.action) {
                        DragEvent.ACTION_DRAG_STARTED -> {
                            if(respondToDrag) {
                                iconLayout.setBackgroundResource(R.drawable.icon_drag_target)
                            }
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            if(respondToDrag) {
                                iconLayout.setBackgroundColor(enteredColor)
                            }
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            if(respondToDrag) {
                                iconLayout.setBackgroundResource(R.drawable.icon_drag_target)
                            }
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            iconLayout.setBackgroundColor(Color.TRANSPARENT)
                        }
                        DragEvent.ACTION_DROP -> {
                            if(respondToDrag) {
                                val id = dragEvent.clipData.getItemAt(0).text.toString()
                                launchInfo = dragAndDropData.retrieveLaunchInfo(id)
                                setupIcon()
                                listener.onIconChanged()
                            }
                        }
                    }
                }
                true
            }
        }
    }

    private fun setupAttrs(attrs: AttributeSet){
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.Icon, 0, 0)
        dragTarget = array.getBoolean(R.styleable.Icon_dragTarget, true)
        blankOnDrag = array.getBoolean(R.styleable.Icon_blankOnDrag, false)
    }

    fun setupIcon() {
        if (launchInfo.getActivityName() != "") {
            icon.setImageDrawable(appList.getIconFromApps(launchInfo))
            label.text = appList.getLabelFromApps(launchInfo)

            icon.setOnClickListener {
                listener.onLaunch(launchInfo, displayId)
            }

            icon.setOnLongClickListener { view ->
                val id = System.currentTimeMillis().toString()
                val passedLaunchInfo = launchInfo.copy()
                dragAndDropData.addLaunchInfo(passedLaunchInfo, id)
                var clipData = ClipData.newPlainText("launchInfo", id)

                listener.onDragStarted(view, clipData)

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
                listener.onLongClick()
                true
            }
            label.setOnLongClickListener { view ->
                listener.onLongClick()
                true
            }
            icon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            label.text = ""
        }
    }

    fun setListener(iconInterface: IconInterface){
        listener = iconInterface
    }

    fun setLaunchInfo(appActivityName: String, packageName:String, userSerial: Long){
        launchInfo.setActivityName(appActivityName)
        launchInfo.setPackageName(packageName)
        launchInfo.setUserSerial(userSerial)
        setupIcon()
    }

    fun setBlankOnDrag(state: Boolean){
        blankOnDrag = state
    }

    interface IconInterface{
        fun onIconChanged()
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onLongClick()
    }
}