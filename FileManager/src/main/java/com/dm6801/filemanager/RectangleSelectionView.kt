package com.dm6801.filemanager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.absoluteValue

class RectangleSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        val TAG = this::class.java.enclosingClass?.simpleName!!
        private const val SKIP_PX = 10f
    }

    private var x1 = -1f
    private var x2 = -1f
    private var y1 = -1f
    private var y2 = -1f
    private val dx get() = x2 - x1
    private val dy get() = y2 - y1
    val isMoving get() = x2 != -1f && dx.absoluteValue > 10f && y2 != -1f && dy.absoluteValue > 10f

    private var strokeWidth: Float = 0f
    private lateinit var strokePaint: Paint
    private lateinit var fillPaint: Paint

    init {
        init(
            fillColor = Color.parseColor("#228888EE"),
            strokeColor = Color.parseColor("#44777777"),
            strokeWidth = 1f
        )
    }

    fun init(fillColor: Int?, strokeColor: Int?, strokeWidth: Float = 3f) {
        fillColor?.let(::setFill)
        strokeColor?.let { setStroke(strokeColor, strokeWidth) }
    }

    private fun setStroke(color: Int, width: Float = 3f) {
        strokeWidth = width
        strokePaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = width
        }
    }

    private fun setFill(color: Int) {
        fillPaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null || dx == 0f || dy == 0f) return
        try {
            canvas.drawRect(x1, y1, x2, y2, strokePaint)
            canvas.drawRect(
                (x1 + strokeWidth).coerceAtMost(x2),
                (y1 + strokeWidth).coerceAtMost(y2),
                (x2 - strokeWidth).coerceAtLeast(x1),
                (y2 - strokeWidth).coerceAtLeast(y1),
                fillPaint
            )
        } catch (_: Throwable) {
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    fun onMotionEvent(ev: MotionEvent?): Boolean? {
        return ev?.parse()
    }

    private fun MotionEvent.parse(): Boolean? {
        return when (actionMasked) {
            MotionEvent.ACTION_DOWN -> onActionDown(this)
            MotionEvent.ACTION_MOVE -> onActionMove(this)
            MotionEvent.ACTION_UP -> onActionUp(this)
            else -> false
        }
    }

    fun onActionDown(ev: MotionEvent?): Boolean {
        ev ?: return false
        Log.d(TAG, "MotionEvent.ACTION_DOWN\t$this")
        x1 = ev.x
        y1 = ev.y
        return true
    }

    fun onActionMove(ev: MotionEvent?): Boolean {
        ev ?: return false
        Log.d(TAG, "MotionEvent.ACTION_MOVE\t$this")
        if ((ev.x - x2).absoluteValue < SKIP_PX || (ev.y - y2).absoluteValue < SKIP_PX) return false
        x2 = ev.x
        y2 = ev.y
        invalidate()
        return true
    }

    fun onActionUp(ev: MotionEvent?): Boolean {
        ev ?: return false
        Log.d(TAG, "MotionEvent.ACTION_UP\t$this")
        x2 = ev.x
        y2 = ev.y
        invalidate()
        x1 = -1f
        x2 = -1f
        y1 = -1f
        y2 = -1f
        return true
    }

}