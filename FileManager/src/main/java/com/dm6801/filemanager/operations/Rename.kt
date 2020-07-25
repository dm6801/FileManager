package com.dm6801.filemanager.operations

import java.io.File

class Rename(
    private val source: String,
    private val newName: String
) : Operation(listOf(source), null, null) {

    override val requireFileExists = true
    override val requireDestinationPath = false

    override suspend fun fileAction(
        file: File,
        onFileExists: suspend (name: String?) -> String?
    ) {
        val target = File(source, newName)
        if (!target.exists()) file.renameTo(target)
    }

}