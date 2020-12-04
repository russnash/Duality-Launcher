package us.graymatterapps.dualitylauncher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.dual_launch.view.*
import kotlinx.android.synthetic.main.folder.view.*
import kotlin.math.floor

class HomeLayout(context: Context, attributeSet: AttributeSet?) : ViewGroup(
    context,
    attributeSet
) {
    private var numRows = 1
    private var numColumns = 1
    private var cellWidth = 1
    private var cellHeight = 1
    private var dragImage = ImageView(context)
    var page: Int = 9999
    lateinit var parentActivity: MainActivity
    val TAG = javaClass.simpleName

    init {
        if (attributeSet != null) {
            setupAttributes(attributeSet)
        }
        dragImage.setBackgroundResource(R.drawable.icon_drag_target)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        this.setOnDragListener { view, dragEvent ->
            if (dragEvent != null) {
                var dragType: String? = null
                if(dragEvent.clipDescription != null) {
                    dragType = dragEvent.clipDescription.label.toString()
                }

                when (dragEvent.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val params = findClosestCell(dragEvent.x, dragEvent.y)
                        if (dragType == "launchInfo") {
                            dragImage.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
                        }
                        if (dragType == "widget") {
                            dragImage.setImageBitmap(dragWidgetBitmap)
                            params.columnSpan = widthToCells(dragWidgetBitmap!!.width)
                            params.rowSpan = heightToCells(dragWidgetBitmap!!.height)
                        }
                        dragImage.layoutParams = params
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        this.addView(dragImage)
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        this.removeView(dragImage)
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        this.removeView(dragImage)
                    }
                    DragEvent.ACTION_DRAG_LOCATION -> {
                        val params = findClosestCell(dragEvent.x, dragEvent.y)
                        if(dragType == "widget") {
                            params.columnSpan = widthToCells(dragWidgetBitmap!!.width)
                            params.rowSpan = heightToCells(dragWidgetBitmap!!.height)
                        }
                        dragImage.layoutParams = params
                    }
                    DragEvent.ACTION_DROP -> {
                        if (dragType == "launchInfo") {
                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                            val info = dragAndDropData.retrieveLaunchInfo(id)
                            if (info.getType() == LaunchInfo.ICON) {
                                val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
                                val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
                                val iconPadding = settingsPreferences.getInt("home_icon_padding", 5)
                                val textSize = settingsPreferences.getInt("home_text_size", 14)
                                val icon = Icon(parentActivity, null, true, page)
                                val params = dragImage.layoutParams as HomeLayout.LayoutParams
                                params.columnSpan = 1
                                params.rowSpan = 1
                                params.freeForm = false
                                icon.layoutParams = params
                                icon.label.setTextColor(textColor)
                                icon.label.setShadowLayer(6F, 0F, 0F, textShadowColor)
                                icon.label.textSize = textSize.toFloat()
                                icon.setListener(parentActivity.homePagerAdapter as Icon.IconInterface)
                                icon.setLaunchInfo(info)
                                icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                                this.addView(icon, params)
                            }
                            if(info.getType() == LaunchInfo.DUALLAUNCH){
                                val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
                                val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
                                val iconPadding = settingsPreferences.getInt("home_icon_padding", 5)
                                val textSize = settingsPreferences.getInt("home_text_size", 14)
                                val dualLaunch = DualLaunch(parentActivity, null, info.getDualLaunchName(), info, true, page)
                                val params = dragImage.layoutParams as HomeLayout.LayoutParams
                                params.columnSpan = 1
                                params.rowSpan = 1
                                params.freeForm = false
                                dualLaunch.layoutParams = params
                                dualLaunch.dualLaunchLabel.maxLines = 1
                                dualLaunch.dualLaunchLabel.setTextColor(textColor)
                                dualLaunch.dualLaunchLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
                                dualLaunch.dualLaunchLabel.textSize = textSize.toFloat()
                                dualLaunch.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                                dualLaunch.setListener(parentActivity.homePagerAdapter as DualLaunch.DualLaunchInterface)
                                this.addView(dualLaunch, params)
                            }
                            if (info.getType() == LaunchInfo.FOLDER) {
                                val textColor = settingsPreferences.getInt("home_text_color", Color.WHITE)
                                val textShadowColor = settingsPreferences.getInt("home_text_shadow_color", Color.BLACK)
                                val textSize = settingsPreferences.getInt("home_text_size", 14)
                                val iconPadding = settingsPreferences.getInt("home_icon_padding", 5)
                                val folder = Folder(
                                    parentActivity,
                                    null,
                                    info.getFolderName(),
                                    info,
                                    true,
                                    page
                                )
                                val params = dragImage.layoutParams as HomeLayout.LayoutParams
                                params.columnSpan = 1
                                params.rowSpan = 1
                                params.freeForm = false
                                folder.layoutParams = params
                                folder.folderLabel.setTextColor(textColor)
                                folder.folderLabel.setShadowLayer(6F, 0F, 0F, textShadowColor)
                                folder.folderLabel.textSize = textSize.toFloat()
                                folder.setListener(parentActivity.homePagerAdapter as Folder.FolderInterface)
                                folder.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                                this.addView(folder, params)
                            }
                            parentActivity.persistGrid(page)
                        }
                        if (dragType == "widget") {
                            val id = dragEvent.clipData.getItemAt(0).text.toString()
                            val widgetInfo = dragAndDropData.retrieveWidgetId(id)
                            val widgetContainer = WidgetContainer(
                                parentActivity,
                                widgetInfo.getAppWidgetId(),
                                widgetInfo.getAppWidgetProviderInfo()
                            )
                            val params = dragImage.layoutParams as HomeLayout.LayoutParams
                            widgetContainer.layoutParams = params
                            widgetContainer.setListener(parentActivity as WidgetContainer.WidgetInterface)
                            this.addView(widgetContainer, params)
                        }
                    }
                }
            }
            true
        }
    }

    private fun findClosestCell(xpos: Float, ypos: Float): HomeLayout.LayoutParams {
        val params = HomeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val column = floor(xpos / cellWidth)
        val row = floor(ypos / cellHeight)
        params.column = (column).toInt()
        params.row = (row).toInt()
        params.freeForm = false
        return params
    }

    fun setGridSize(rows: Int, columns: Int) {
        numRows = rows
        numColumns = columns
        this.invalidate()
    }

    fun getRows(): Int {
        return numRows
    }

    fun getColumns(): Int {
        return numColumns
    }

    private fun setupAttributes(attributeSet: AttributeSet) {
        val array = context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.HomeLayout,
            0,
            0
        )
        numRows = array.getInteger(R.styleable.HomeLayout_numRows, 1)
        numColumns = array.getInteger(R.styleable.HomeLayout_numColumns, 1)
    }

    private fun setCellSizes(width: Int, height: Int) {
        cellWidth = width / numColumns
        cellHeight = height / numRows
        Log.d(TAG, "HomeLayout.setCellSizes cellWidth:$cellWidth cellHeight:$cellHeight")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout(), ${this.childCount} children")
        for (n in 0 until this.childCount) {
            val child = this.getChildAt(n)
            if(child is WidgetContainer) {
                Log.d(TAG, "WidgetContainer")
            }
            val params = child.layoutParams as HomeLayout.LayoutParams
            val childWidth = params.columnSpan * cellWidth
            val childHeight = params.rowSpan * cellHeight
            val widthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            child.measure(widthSpec, heightSpec)
            var type: String = ""
            when (child) {
                is Icon -> {
                    type = "Icon"
                }
                is Folder -> {
                    type = "Folder"
                }
                is WidgetContainer -> {
                    type = "WidgetContainer"
                }
                else -> {
                    type = "Unknown"
                }
            }
            Log.d(TAG, "onLayout() $type row:${params.row} column:${params.column} rowSpan:${params.rowSpan} columnSpan:${params.columnSpan}")
            layoutChild(
                child,
                params.row,
                params.column,
                params.rowSpan,
                params.columnSpan,
                params.freeForm
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setCellSizes(widthSize, heightSize)
        setMeasuredDimension(widthSize, heightSize)
    }

    private fun layoutChild(
        view: View,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
        freeForm: Boolean
    ) {
        var l = 0
        var t = 0
        var r = 0
        var b = 0
        if (freeForm) {
            l = column
            t = row
            r = l + (columnSpan * cellWidth)
            b = t + (rowSpan * cellHeight)
        } else {
            l = column * cellWidth
            t = row * cellHeight
            r = l + (columnSpan * cellWidth)
            b = t + (rowSpan * cellHeight)
        }
        view.layout(l, t, r, b)
    }

    fun widthToCells(width: Int): Int {
        return (width + cellWidth - 1) / cellWidth
    }

    fun heightToCells(height: Int): Int {
        return (height + cellHeight - 1) / cellHeight
    }

    fun getCellWidth(): Int {
        return cellWidth
    }

    fun getCellHeight(): Int {
        return cellHeight
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return super.checkLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return super.generateDefaultLayoutParams() as HomeLayout.LayoutParams
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return super.generateLayoutParams(attrs) as HomeLayout.LayoutParams
    }

    class LayoutParams : ViewGroup.LayoutParams {
        constructor(width: Int, height: Int) : super(width, height) {}
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
            val array = context!!.theme.obtainStyledAttributes(
                attrs,
                R.styleable.HomeLayout_LayoutParams,
                0,
                0
            )
            row = array.getInteger(R.styleable.HomeLayout_LayoutParams_row, 1)
            column = array.getInteger(R.styleable.HomeLayout_LayoutParams_column, 1)
            freeForm = array.getBoolean(R.styleable.HomeLayout_LayoutParams_freeForm, false)
            rowSpan = array.getInteger(R.styleable.HomeLayout_LayoutParams_rowSpan, 1)
            columnSpan = array.getInteger(R.styleable.HomeLayout_LayoutParams_columnSpan, 1)
        }

        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1
        var freeForm = false
    }
}