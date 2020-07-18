package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

internal fun <T> List<T>.mutate(action: MutableList<T>.() -> Unit): List<T> {
    return try {
        (this as? MutableList)?.apply(action)
    } catch (_: Throwable) {
        null
    } ?: toMutableList().apply(action)
}

internal val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    throwable.printStackTrace()
}

internal fun CoroutineScope?.safeLaunch(
    context: CoroutineContext = Dispatchers.Main,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return (this ?: CoroutineScope(context)).launch(context + exceptionHandler) {
        try {
            block()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

@SuppressLint("DefaultLocale")
fun getMimeType(path: String, context: Context? = null, fallback: String = "*/*"): String {
    return MimeTypeMap.getFileExtensionFromUrl(path)
        ?.run { MimeTypeMap.getSingleton().getMimeTypeFromExtension(toLowerCase()) }
        ?: context?.run { contentResolver.getType(Uri.parse(path)) }
        ?: fallback
}

internal fun View.enable() {
    alpha = 1f
    isEnabled = true
}

internal fun View.disable() {
    alpha = 0.7f
    isEnabled = false
}

internal fun View?.contains(x: Int, y: Int): Boolean {
    this ?: return false
    if (!isVisible) return false
    val rect = Rect()
    val viewLocation = intArrayOf(0, 0)
    getDrawingRect(rect)
    getLocationOnScreen(viewLocation)
    rect.offset(viewLocation[0], viewLocation[1])
    val hitRect = Rect()
    getGlobalVisibleRect(hitRect)
    return rect.contains(x, y)
}

fun View.hitRect(): Rect {
    return Rect().apply { getHitRect(this) }
}