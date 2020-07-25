@file:Suppress("DEPRECATION")

package com.dm6801.filemanager

class FileObserver(path: String, val action: (event: Int, path: String?) -> Unit) :
    android.os.FileObserver(path) {
    override fun onEvent(event: Int, path: String?) {
        action(event, path)
    }
}

/*
class FileObserver(file: File, val onEvent: (event: Int, path: String?) -> Unit) {

    private var fileObserver: android.os.FileObserver?

    init {
        fileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            object : android.os.FileObserver(file) {
                override fun onEvent(event: Int, path: String?) {
                    this@FileObserver.onEvent(event, path)
                }
            }
        else object : android.os.FileObserver(file.absolutePath) {
            override fun onEvent(event: Int, path: String?) {
                this@FileObserver.onEvent(event, path)
            }
        }
        fileObserver?.startWatching()
    }

    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
    }

}*/
