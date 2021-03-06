package com.dm6801.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun Context.launchOpenFile(path: String) = CoroutineScope(Dispatchers.Main).safeLaunch {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(path, this@launchOpenFile) ?: return@safeLaunch,
                getMimeType(path, this@launchOpenFile) ?: "*/*"
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        startActivity(intent)
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

fun Context.launchOpenFilePicker(path: String) = CoroutineScope(Dispatchers.Main).safeLaunch {
    try {
        val intentTransform: Intent.(Uri) -> Intent =
            { uri ->
                apply {
                    setDataAndType(
                        uri,
                        getMimeType(path, this@launchOpenFilePicker) ?: "*/*"
                    )
                }
            }
        //val uri = Uri.parse(path)
        val uri = FileProvider.getUriForFile(path, this@launchOpenFilePicker) ?: return@safeLaunch
        val intent = Intent(Intent.ACTION_VIEW).intentTransform(uri).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        //var intent = Intent(Intent.ACTION_VIEW).intentTransform(uri)
        //if (intent.resolveActivity(packageManager) == null)
        //  intent = Intent(Intent.ACTION_OPEN_DOCUMENT).intentTransform(uri)
        startActivity(Intent.createChooser(intent, "open file"))
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}