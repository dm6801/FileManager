package com.dm6801.filemanager.operations

import android.util.Log
import java.io.File

class Create(
    paths: List<String>?,
    type: Type?
) : Operation(paths, null, type) {

    override suspend fun fileAction(
        file: File,
        onFileExists: (suspend (name: String) -> String?)?
    ) {
        when (type) {
            Type.File -> createFile(file, onFileExists)
            Type.Folder -> createFolder(file, onFileExists)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun createFile(file: File, onFileExists: (suspend (name: String) -> String?)?) {
        file.ensurePathExists()
        if (!file.exists()) {
            file.createNewFile()
        } else {
            val newName = onFileExists?.invoke(file.name)
            if (newName != null) { //rename
                val newFile = File(file.parent, newName)
                newFile.createNewFile()
            } else { //overwrite
                file.delete()
                file.createNewFile()
            }
        }

    }

    private suspend fun createFolder(
        file: File,
        onFileExists: (suspend (name: String) -> String?)?
    ) {
        file.ensurePathExists()
        if (!file.exists()) {
            file.mkdirs()
        } else {
            val newPath = onFileExists?.invoke(file.name)
            if (newPath != null) { //rename
                val newFile = File(file.parent, newPath)
                newFile.mkdirs()
            } else { //overwrite
                file.delete()
                file.mkdirs()
            }
        }
    }

    override fun logOperation() {
        Log.d(
            javaClass.superclass?.simpleName ?: javaClass.simpleName,
            "executing ${javaClass.simpleName} on ${type?.name}..."
        )
    }

}