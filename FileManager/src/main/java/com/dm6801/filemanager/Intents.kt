package com.dm6801.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun Context.launchOpenFile(path: String) = CoroutineScope(Dispatchers.Main).safeLaunch {
    try {
        val intentTransform: Intent.(Uri) -> Intent =
            { uri -> apply { setDataAndType(uri, getMimeType(path, this@launchOpenFile)) } }
        val uri = Uri.parse(path)
        var intent = Intent(Intent.ACTION_VIEW).intentTransform(uri)
        //if (intent.resolveActivity(packageManager) == null)
        //  intent = Intent(Intent.ACTION_OPEN_DOCUMENT).intentTransform(uri)
        startActivity(Intent.createChooser(intent, "open file"))
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}