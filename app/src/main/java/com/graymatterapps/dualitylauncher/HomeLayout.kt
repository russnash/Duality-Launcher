package com.graymatterapps.dualitylauncher

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup

class HomeLayout(context: Context, attributeSet: AttributeSet?) : ViewGroup(
    context,
    attributeSet
) {
    private var numRows = 1
    private var numColumns = 1
    private var cellWidth = 1
    private var cellHeight = 1
    val TAG = "HomeLayout"

    init{
        if(attributeSet != null){
            setupAttributes(attributeSet)
        }
    }

    fun setGridSize(rows: Int, columns: Int){
        numRows = rows
        numColumns = columns
        this.invalidate()
    }

    fun getRows() : Int {
        return numRows
    }

    fun getColumns() : Int {
        return numColumns
    }

    private fun setupAttributes(attributeSet: AttributeSet){
        val array = context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.HomeLayout,
            0,
            0
        )
        numRows = array.getInteger(R.styleable.HomeLayout_numRows, 1)
        numColumns = array.getInteger(R.styleable.HomeLayout_numColumns, 1)
    }

    private fun setCellSizes(width: Int, height: Int){
        cellWidth = width / numColumns
        cellHeight = height / numRows
        Log.d(TAG, "HomeLayout.setCellSizes cellWidth:$cellWidth cellHeight:$cellHeight")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout, ${this.childCount} children")
        for(n in 0 until this.childCount){
            val child = this.getChildAt(n)
            val params = child.layoutParams as HomeLayout.LayoutParams
            val childWidth = params.columnSpan * cellWidth
            val childHeight = params.rowSpan * cellHeight
            val widthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            child.measure(widthSpec, heightSpec)
            //Log.d(TAG, "row:${params.row} column:${params.column} rowSpan:${params.rowSpan} columnSpan:${params.columnSpan}")
            layoutChild(child, params.row, params.column, params.rowSpan, params.columnSpan)
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

    private fun layoutChild(view: View, row: Int, column: Int, rowSpan: Int, columnSpan: Int){
        val l = column * cellWidth
        val t = row * cellHeight
        val r = l + (columnSpan * cellWidth)
        val b = t + (rowSpan * cellHeight)
        view.layout(l, t, r, b)
        //view.layoutParams.width = (columnSpan * cellWidth)
        //view.layoutParams.height = (rowSpan * cellHeight)
    }

    fun widthToCells(width: Int) : Int{
        return (width + cellWidth -1) / cellWidth
    }

    fun heightToCells(height: Int) : Int{
        return (height + cellHeight -1) / cellHeight
    }

    fun getCellWidth() : Int {
        return cellWidth
    }

    fun getCellHeight() : Int {
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
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1
    }
}