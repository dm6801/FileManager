package com.dm6801.filemanager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.nio.charset.Charset

class FileInfo {

    companion object {
        fun getDirectoryInfo(path: String): Deferred<String?> =
            CoroutineScope(Dispatchers.IO).async {
                try {
                    //val _path = if (path.startsWith("/")) path else "/$path"
                    val process =
                        ProcessBuilder("ls -l")
                            .redirectErrorStream(true)
                            .start()
                    //val process = Runtime.getRuntime().exec("ls -a")

                    return@async process.inputStream.readBytes().toString(Charset.defaultCharset())
                        .also { process.waitFor() }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }
    }

}