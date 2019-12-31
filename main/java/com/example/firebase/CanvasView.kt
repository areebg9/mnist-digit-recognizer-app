package com.example.firebase

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import androidx.core.content.res.ResourcesCompat

// Created by Qwerty71

private const val STROKE_WIDTH = 60f

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    val TAG = "CanvasView"

    private lateinit var button: Button

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private var currentX = 0f
    private var currentY = 0f

    private var newX = 0f
    private var newY = 0f

    var widthTotal = 0
    var heightTotal = 0

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
    private val paint = Paint().apply{
        color = drawColor
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH
    }
    private var path = Path()

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {

        super.onSizeChanged(width, height, oldWidth, oldHeight)

        if (::extraBitmap.isInitialized) extraBitmap.recycle()

        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)

        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        newX = event.x
        newY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }

        return true
    }

    private fun touchStart(){
        path.reset()
        path.moveTo(newX, newY)
        currentX = newX
        currentY = newY
    }

    private fun touchMove(){
        val dx = Math.abs(newX - currentX)
        val dy = Math.abs(newY - currentY)

        if (dx >= touchTolerance || dy >= touchTolerance){
            path.quadTo(currentX, currentY, (newX + currentX)/2, (newY + currentY)/2)
            currentX = newX
            currentY = newY

            extraCanvas.drawPath(path, paint)
        }

        invalidate()
    }

    private fun touchUp(){
        path.reset()
    }

    public fun getBitmap(): Bitmap {
        return extraBitmap
    }

    public fun clear(): Void? {
        extraCanvas.drawColor(backgroundColor)
        return null
    }
}
