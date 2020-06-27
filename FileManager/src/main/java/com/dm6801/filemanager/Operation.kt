package com.dm6801.filemanager

import android.util.Log
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.properties.Delegates

data class Operation(
    val type: Type,
    val files: List<String>?,
    var destinationPath: String?
) {
    enum class Type {
        Create, Copy, Move, Delete
    }

    private var observers: MutableList<Observer<Boolean>> = mutableListOf()

    var isExecuted by Delegates.vetoable(false) { _, oldValue, newValue ->
        if (oldValue) false
        else {
            observers.forEach { it.onChanged(newValue) }
            true
        }
    }
        private set

    fun observe(observer: Observer<Boolean>): Operation {
        observers.add(observer)
        return this
    }

    fun execute(onFileExists: (suspend (name: String) -> String?)? = null) {
        if (isExecuted) return
        CoroutineScope(Dispatchers.IO).safeLaunch {
            when (type) {
                Type.Create -> {
                    files?.forEach { path ->
                        val file = File(path).ensurePathExists()
                        try {
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
                            isExecuted = true
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            isExecuted = false
                        }
                    }
                }
                Type.Copy -> { //TODO
                    val destPath = destinationPath ?: return@safeLaunch
                    Log.d(javaClass.simpleName, "copy files")
                    files?.forEach { file ->
                        val dest = destPath + "/" + file.substringAfterLast("/")
                        Log.d(javaClass.simpleName, "$file -> $dest")
                    }
                }
                Type.Move -> { //TODO
                    val destPath = destinationPath ?: return@safeLaunch
                    Log.d(javaClass.simpleName, "move files")
                    files?.forEach { file ->
                        val dest = destPath + "/" + file.substringAfterLast("/")
                        Log.d(javaClass.simpleName, "$file -> $dest")
                    }
                }
                Type.Delete -> {
                    files?.forEach { path ->
                        try {
                            val file = File(path)
                            if (!file.exists()) return@safeLaunch
                            file.deleteRecursively()
                            isExecuted = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isExecuted = false
                        }
                    }
                }
            }
        }
    }

    private fun File.ensurePathExists(): File {
        try {
            parentFile?.mkdirs()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return this
    }
}
