package com.dm6801.filemanager.operations

import java.io.File

class Delete(paths: List<String>) : Operation(paths, null, null) {

    override val requireFileExists = true

    override suspend fun fileAction(
        file: File,
        onFileExists: suspend (name: String?) -> String?
    ) {
        file.deleteRecursively()
    }

}