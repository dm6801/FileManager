package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import java.io.File
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
fun getMimeType(path: String, context: Context? = null): String? {
    var mimeType = MimeTypeMap.getFileExtensionFromUrl(path)
        ?.run { MimeTypeMap.getSingleton().getMimeTypeFromExtension(toLowerCase()) }
    if (!mimeType.isNullOrBlank()) return mimeType
    mimeType = context?.run {
        val uri = Uri.parse(path)
        when (uri.scheme) {
            "content" -> contentResolver.getType(uri)
            else -> FileProvider.getUriForFile(path, context)?.run(contentResolver::getType)
        }
    }
    if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream") return mimeType
    return MimeType.analyze(path)
}

fun File.isImage(): Boolean {
    return try {
        inputStream().use {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(absolutePath, options)
            options.outWidth != -1 && options.outHeight != -1
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }
}

fun File.isVideo(): Boolean {
    return name.endsWith(".mp4") || name.endsWith(".mkv")
}

fun File.isAudio(): Boolean {
    return name.endsWith(".mp3")
}

private val Context.imm: InputMethodManager? get() = getSystemService()

fun EditText.edit() = CoroutineScope(Dispatchers.Main).launch {
    setSelection(text?.length ?: 0)
    requestFocus()
    context.imm?.showSoftInput(this@edit, InputMethodManager.SHOW_IMPLICIT)
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