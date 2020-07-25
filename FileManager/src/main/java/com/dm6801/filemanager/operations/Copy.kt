package com.dm6801.filemanager.operations

import android.util.Log
import java.io.File

class Copy(paths: List<String>) : Operation(paths, null, null) {

    override val requireDestinationPath = true

    override suspend fun fileAction(
        file: File,
        onFileExists: suspend (name: String?) -> String?
    ) {
        val temp = File(destinationPath!!, file.name)
        if (!temp.exists()) {
            file.copyTo(temp)
        } else {
            val newName = onFileExists(file.name)
            if (newName.isNullOrBlank()) return
            val target = File(temp.parent, newName)
            if (file.absolutePath != target.absolutePath)
                file.copyTo(target)
        }
    }

    private fun File.copyTo(target: File) {
        when {
            isDirectory -> {
                val sources = listFiles()
                Log.d(TAG, "\ntarget: $target\nsource: ${sources?.joinToString("\n")}")
                if (!target.exists()) target.mkdirs()
                sources?.forEach { source ->
                    source.copyRecursively(File(target, source.name), overwrite = true)
                }
            }
            isFile -> {
                Log.d(TAG, "\ntarget: $target\nsource: $this")
                copyTo(target, overwrite = true)
            }
        }
    }

}