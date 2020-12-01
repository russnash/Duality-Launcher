package us.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import us.graymatterapps.graymatterutils.GrayMatterUtils

class DualLaunch(
    private val parentActivity: MainActivity,
    attrs: AttributeSet?,
    name: String,
    info: LaunchInfo? = null,
    var replicate: Boolean = false,
    var page: Int
) : LinearLayout(parentActivity, attrs), SharedPreferences.OnSharedPreferenceChangeListener {

    private val dualLaunchLayout: LinearLayout
    private val dualLaunchIcon: ImageView
    private val dualLaunchLabel: TextView

    @Serializable
    private var dualLaunchApps = ArrayList<LaunchInfo>()
    private var launchInfo: LaunchInfo = LaunchInfo()
    private lateinit var listener: DualLaunchInterface
    lateinit var parentLayout: HomeLayout
    private val touchSlop = 7
    private val longClickTime = android.view.ViewConfiguration.getLongPressTimeout()
    private lateinit var menu: PopupMenu
    private var isPopupMenuVisible: Boolean = false
    private var downTime: Long = 0
    val TAG = javaClass.simpleName

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)

        inflate(parentActivity, R.layout.dual_launch, this)
        dualLaunchLayout = findViewById(R.id.dualLaunchLayout)
        dualLaunchIcon = findViewById(R.id.dualLaunchImage)
        dualLaunchLabel = findViewById(R.id.dualLaunchLabel)

        dualLaunchIcon.setImageResource(R.drawable.ic_dual_launch)
        dualLaunchLabel.text = name
        dualLaunchApps.add(LaunchInfo())
        dualLaunchApps.add(LaunchInfo())

        if (info == null) {
            launchInfo.setType(LaunchInfo.DUALLAUNCH)
            launchInfo.setDualLaunchUniqueId(System.currentTimeMillis())
            dualLaunchLabel.text = "New Dual Launch"
            launchInfo.setDualLaunchName("New Dual Launch")
        } else {
            launchInfo = info
            dualLaunchLabel.text = launchInfo.getDualLaunchName()
            depersistDualLaunchApps()
            dualLaunchIcon.setImageBitmap(makeDualLaunchIcon())
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            Log.d(TAG, "onTouchEvent() ${MotionEvent.actionToString(event.action)}")
            if (downTime > 0 && System.currentTimeMillis() - downTime > longClickTime) {
                downTime = 0
                showPopupMenu()
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - downTime < longClickTime) {
                        launch()
                    }
                    downTime = 0
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPopupMenuVisible) {
                        if (event.historySize != 0) {
                            val distance = GrayMatterUtils.getDistance(
                                event.getHistoricalX(0),
                                event.getHistoricalY(0),
                                event.getX(),
                                event.getY()
                            )
                            Log.d(TAG, "distance = $distance, touchSlop = $touchSlop")
                            if (distance > touchSlop) {
                                downTime = 0
                                menu.dismiss()
                                startDragging()
                            }
                            if (distance > 2) {
                                downTime = 0
                            }
                        }
                    }
                    return true
                }
            }
        }
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this.parent is HomeLayout) {
            parentLayout = this.parent as HomeLayout
            if (replicate) {
                val params = this.layoutParams as HomeLayout.LayoutParams
                replicator.addDualLaunch(
                    parentActivity.displayId,
                    launchInfo,
                    page,
                    params.row,
                    params.column
                )
            }
        }

        if(launchInfo.getDualLaunchName() == "New Dual Launch" && replicate) {
            showDualLaunch()
        }
    }

    private fun showPopupMenu() {
        GrayMatterUtils.vibrate(parentActivity, 50)
        menu = PopupMenu(parentActivity, this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            menu.setForceShowIcon(true)
        }

        val menuItemRemove = menu.menu.add(1, 1, 1, "Remove")
        menuItemRemove.setIcon(R.drawable.ic_remove)
        menuItemRemove.setOnMenuItemClickListener {
            removeIcon()
            true
        }

        val menuItemEdit = menu.menu.add(1, 2, 1, "Edit Dual Launch")
        menuItemEdit.setIcon(R.drawable.ic_edit)
        menuItemEdit.setOnMenuItemClickListener {
            showDualLaunch()
            true
        }

        menu.setOnDismissListener {
            isPopupMenuVisible = false
        }
        isPopupMenuVisible = true
        menu.show()
    }

    private fun startDragging() {
        val id = System.currentTimeMillis().toString()
        val passedLaunchInfo = launchInfo.copy()
        dragAndDropData.addLaunchInfo(passedLaunchInfo, id)
        var clipData = ClipData.newPlainText("launchInfo", id)
        listener.onDragStarted(this, clipData)
        convertToEmpty()
    }

    fun makeDualLaunchIcon(): Bitmap {
        val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val drawable: Drawable =
            ContextCompat.getDrawable(parentActivity, R.drawable.ic_launcher_background)!!
        var bitmap = drawableToBitmap(drawable)
        var canvas = Canvas(bitmap!!)
        canvas.drawRect(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat(), clearPaint)

        var firstBitmap =
            ContextCompat.getDrawable(parentActivity, R.drawable.ic_dual_launch)!!.toBitmap()
        if (dualLaunchApps.size != 0) {
            firstBitmap = appList.getIcon(dualLaunchApps[0]).toBitmap()
        }
        var srcRect: Rect = Rect(0, 0, firstBitmap.width, firstBitmap.height)
        var dstRect: Rect = Rect(
            0, 0, (canvas.width * 0.66).toInt(),
            (canvas.height * 0.66).toInt()
        )
        canvas.drawBitmap(firstBitmap, srcRect, dstRect, null)

        var secondBitmap =
            ContextCompat.getDrawable(parentActivity, R.drawable.ic_dual_launch)!!.toBitmap()
        if (dualLaunchApps.size != 0) {
            secondBitmap = appList.getIcon(dualLaunchApps[dualLaunchApps.size - 1]).toBitmap()
        }
        srcRect = Rect(0, 0, secondBitmap.width, secondBitmap.height)
        dstRect = Rect(canvas.width / 3, canvas.height / 3, canvas.width, canvas.height)
        canvas.drawBitmap(secondBitmap, srcRect, dstRect, null)

        var plus =
            ContextCompat.getDrawable(parentActivity, R.drawable.ic_plus)!!.toBitmap()
        srcRect = Rect(0, 0, plus.width, plus.height)
        dstRect = Rect(0, canvas.height - plus.height, (canvas.width * 0.33).toInt(), canvas.height)

        canvas.drawBitmap(plus, srcRect, dstRect, null)

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

    private fun depersistDualLaunchApps() {
        var tempDualLaunchApps = ArrayList<LaunchInfo>()
        val loadItJson = prefs.getString("duallaunch" + launchInfo.getDualLaunchUniqueId(), "")
        if (loadItJson != "") {
            tempDualLaunchApps = loadItJson?.let { Json.decodeFromString(it) }!!
        }
        dualLaunchApps.clear()
        dualLaunchApps.addAll(tempDualLaunchApps)
        dualLaunchIcon.setImageBitmap(makeDualLaunchIcon())
    }

    private fun persistDualLaunchApps() {
        val saveItJson = Json.encodeToString(dualLaunchApps)
        val editor = prefs.edit()
        editor.putString("duallaunch" + launchInfo.getDualLaunchUniqueId(), saveItJson)
        editor.apply()
        dualLaunchIcon.setImageBitmap(makeDualLaunchIcon())
    }

    fun setDualLaunchName(name: String) {
        dualLaunchLabel.text = name
        launchInfo.setDualLaunchName(name)
        if(::parentLayout.isInitialized) {
            val params = this.layoutParams as HomeLayout.LayoutParams
            replicator.changeDualLaunch(
                parentActivity.displayId,
                launchInfo,
                page,
                params.row,
                params.column
            )
        } else {
            listener.onDualLaunchUpdated()
        }
    }

    fun setLaunchInfo(info: LaunchInfo) {
        launchInfo = info
        dualLaunchLabel.text = info.getDualLaunchName()
    }

    fun getLaunchInfo(): LaunchInfo {
        return launchInfo
    }

    fun addFirstApp(info: LaunchInfo) {
        dualLaunchApps[0] = info
        persistDualLaunchApps()
        makeDualLaunchIcon()
    }

    fun addSecondApp(info: LaunchInfo) {
        dualLaunchApps[1] = info
        persistDualLaunchApps()
        makeDualLaunchIcon()
    }

    private fun showDualLaunch() {
        listener.onShowDualLaunch(true)
        listener.onSetupDualLaunch(
            dualLaunchApps,
            SpannableStringBuilder(dualLaunchLabel.text),
            this
        )
    }

    fun setListener(ear: DualLaunchInterface) {
        listener = ear
    }

    fun launch() {
        appList.launchDualLaunch(parentActivity, dualLaunchApps[0], dualLaunchApps[1])
    }

    fun removeIcon() {
        convertToEmpty()
    }

    fun convertToEmpty() {
        if(::parentLayout.isInitialized) {
            val params = this.layoutParams as HomeLayout.LayoutParams
            replicator.deleteViews(parentActivity.displayId, page, params.row, params.column)
            parentLayout.removeView(this)
            parentActivity.persistGrid(page)
        }
    }

    interface DualLaunchInterface {
        fun onShowDualLaunch(state: Boolean)
        fun onSetupDualLaunch(apps: ArrayList<LaunchInfo>, name: Editable, dualLaunch: DualLaunch)
        fun onDragStarted(view: View, clipData: ClipData)
        fun onDualLaunchUpdated()
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key != null) {
            if (key == "duallaunch" + launchInfo.getDualLaunchUniqueId()) {
                depersistDualLaunchApps()
            }
        }
    }
}