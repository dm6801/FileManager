package com.dm6801.filemanager.operations

import java.io.File

class Copy(
    paths: List<String>?,
    destinationPath: String?
) : Operation(paths, destinationPath, null) {

    override val requireDestinationPath = true

    override suspend fun fileAction(
        file: File,
        onFileExists: (suspend (name: String) -> String?)?
    ) {
        file.copyRecursively(File(destinationPath!!, file.name), overwrite = false)
    }

}