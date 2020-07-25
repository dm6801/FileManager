package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.postDelayed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

open class TouchListener : View.OnTouchListener {

    companion object {
        const val TAG = "TouchListener"
        private val DOUBLE_CLICK_TIMEOUT = (ViewConfiguration.getDoubleTapTimeout() * 0.75).toLong()
        private val LONG_CLICK_TIMEOUT = (ViewConfiguration.getLongPressTimeout()).toLong()
    }

    private val time = AtomicLong(SystemClock.uptimeMillis())
    private var singleClickDelayed: Runnable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                if (SystemClock.uptimeMillis() - event.downTime >= LONG_CLICK_TIMEOUT) {
                    Log.d(TAG, "onLongClick(): v=${v?.id}, event=$event")
                    if (onLongClick(v, event)) return true
                }
                val previousTime = time.getAndSet(event.eventTime)
                if (event.eventTime - previousTime <= DOUBLE_CLICK_TIMEOUT) {
                    singleClickDelayed?.let { v?.removeCallbacks(it) }
                    Log.d(TAG, "onDoubleClick(): v=${v?.id}, event=$event")
                    if (onDoubleClick(v, event)) return true
                }
                singleClickDelayed = v?.postDelayed(DOUBLE_CLICK_TIMEOUT) {
                    Log.d(TAG, "onSingleClick(): v=${v.id}, event=$event")
                    onSingleClick(v, event)
                }
                return true
            }
            else -> false
        }
    }

    open fun onLongClick(v: View?, event: MotionEvent): Boolean {
        return false
    }

    open fun onDoubleClick(v: View?, event: MotionEvent): Boolean {
        return false
    }

    open fun onSingleClick(v: View?, event: MotionEvent): Boolean {
        return false
    }

}