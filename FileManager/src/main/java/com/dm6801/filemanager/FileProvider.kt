package com.dm6801.filemanager

import android.content.Context
import android.net.Uri
import java.io.File

class FileProvider : androidx.core.content.FileProvider() {

    companion object {
        fun getUriForFile(path: String, context: Context): Uri? {
            return try {
                getUriForFile(
                    context,
                    "${context.packageName}.${BuildConfig.LIBRARY_PACKAGE_NAME}.provider",
                    File(path)
                )
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }
        }
    }

}