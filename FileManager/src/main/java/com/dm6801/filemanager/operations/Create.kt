package com.dm6801.filemanager.operations

import java.io.File

class Create(
    paths: List<String>,
    type: Type
) : Operation(paths, null, type) {

    override suspend fun fileAction(
        file: File,
        onFileExists: suspend (name: String?) -> String?
    ) {
        when (type) {
            Type.File -> createFile(file, onFileExists)
            Type.Folder -> createFolder(file, onFileExists)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun createFile(
        file: File,
        onFileExists: suspend (name: String?) -> String?
    ) {
        file.ensurePathExists()
        if (!file.exists()) {
            file.createNewFile()
        } else {
            val newName = onFileExists(file.name)
            if (newName.isNullOrBlank()) return
            val newFile = File(file.parent, newName)
            if (!newFile.exists())
                newFile.createNewFile()
        }

    }

    private suspend fun createFolder(
        file: File,
        onFileExists: suspend (name: String?) -> String?
    ) {
        file.ensurePathExists()
        if (!file.exists()) {
            file.mkdirs()
        } else {
            val newPath = onFileExists(file.name)
            if (newPath.isNullOrBlank()) return
            val newFile = File(file.parent, newPath)
            if (!newFile.exists())
                newFile.mkdirs()
        }
    }

}