package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

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

fun String.extension(): String? {
    val separatorIndex = indexOfLast { it == '/' }.coerceAtLeast(0)
    val dotIndex = indexOf('.', separatorIndex)
    if (dotIndex == -1 || dotIndex + 1 >= length - 1) return null
    return substring(dotIndex + 1)
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

fun File.isVideo(): Boolean { //TODO
    return name.endsWith(".mp4") || name.endsWith(".mkv")
}

fun File.isAudio(): Boolean { //TODO
    return name.endsWith(".mp3")
}

@SuppressLint("DefaultLocale")
fun File.getMetaData(hideEmpty: Boolean = true): Map<String, String?>? {
    return try {
        val mmr = MediaMetadataRetriever()
            .apply { setDataSource(absolutePath) }
        MediaMetadataRetriever::class.java.declaredFields.mapNotNull {
            try {
                it.isAccessible = true
                val value: String? = mmr.extractMetadata(it.getInt(mmr))
                if (value == null && hideEmpty) null
                else it.name.substringAfter("KEY_").toLowerCase() to value
            } catch (_: Throwable) {
                null
            }
        }.toMap()
    } catch (_: Throwable) {
        null
    }
}

@SuppressLint("DefaultLocale")
@Suppress("UNCHECKED_CAST")
fun File.getAttributes(hideEmpty: Boolean = true): Map<String, String?>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    return try {
        val attrsView = Files.getFileAttributeView(toPath(), PosixFileAttributeView::class.java)
        val attrs = attrsView.readAttributes()
        (attrsView.getField("posixAttributeNames") as? Set<String>)
            ?.mapNotNull {
                val value = (attrs.invoke(it) ?: attrs.getField(it))?.toString()
                if (value == null && hideEmpty) null
                else it to value
            }?.toMap()
    } catch (_: Throwable) {
        null
    }
}

@SuppressLint("DefaultLocale")
private fun Any.invoke(name: String, vararg args: Any): Any? {
    return try {
        this.javaClass.declaredMethods.find {
            it.name.toLowerCase().contains(name.toLowerCase())
        }?.run {
            isAccessible = true
            if (this.parameterTypes.isNotEmpty())
                invoke(this@invoke, args)
            else
                invoke(this@invoke)
        }
    } catch (_: Throwable) {
    }
}

@SuppressLint("DefaultLocale")
private fun Any.getField(name: String): Any? {
    return try {
        this.javaClass.declaredFields.find {
            it.name.toLowerCase().contains(name.toLowerCase())
        }?.run {
            isAccessible = true
            get(this@getField)
        }
    } catch (_: Throwable) {
    }
}

@OptIn(ExperimentalTime::class)
fun formatDuration(ms: Long): String {
    return ms.milliseconds.toString()
}

fun formatSize(context: Context?, sizeBytes: Long): String {
    return Formatter.formatFileSize(context, sizeBytes)
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