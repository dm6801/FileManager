package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.webkit.MimeTypeMap
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

fun <T> List<T>.mutate(action: MutableList<T>.() -> Unit): List<T> {
    return try {
        (this as? MutableList)?.apply(action)
    } catch (_: Throwable) {
        null
    } ?: toMutableList().apply(action)
}

val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
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