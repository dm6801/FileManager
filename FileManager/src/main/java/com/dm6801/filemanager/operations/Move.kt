package com.dm6801.filemanager.operations

import java.io.File

class Move(
    paths: List<String>?,
    destinationPath: String?
) : Operation(paths, destinationPath, null) {

    override val requireDestinationPath = true

    override suspend fun fileAction(
        file: File,
        onFileExists: (suspend (name: String) -> String?)?
    ) {
        if (file.isDirectory)
            file.copyRecursively(File(destinationPath!!, file.name), overwrite = false)
        else
            file.copyTo(File(destinationPath!!), overwrite = false)
        file.delete()
    }

}