package com.dm6801.filemanager.operations

import android.util.Log
import androidx.lifecycle.Observer
import com.dm6801.filemanager.exceptionHandler
import com.dm6801.filemanager.safeLaunch
import kotlinx.coroutines.*
import java.io.File
import kotlin.properties.Delegates

abstract class Operation(
    var paths: List<String>?,
    var destinationPath: String?,
    val type: Type?
) {

    private val files = paths?.map { File(it) }

    enum class Type {
        File, Folder
    }

    private var observers: MutableList<Observer<Boolean>> = mutableListOf()

    var isExecuted: Boolean? by Delegates.vetoable<Boolean?>(null) { _, oldValue, newValue ->
        if (oldValue == true) false
        else {
            observers.forEach { it.onChanged(newValue) }
            true
        }
    }
        protected set

    fun observe(observer: Observer<Boolean>): Operation {
        observers.add(observer)
        return this
    }

    protected open val requireDestinationPath: Boolean = false
    protected open val requireFileExists: Boolean = false

    fun execute(onFileExists: (suspend (name: String) -> String?)? = null) {
        if (isExecuted == true) return
        CoroutineScope(Dispatchers.IO).apply {
            launch(exceptionHandler) {
                //isExecuted = try {
                iteratePaths(onFileExists)
                //    true
                //} catch (t: Throwable) {
                //    t.printStackTrace()
                //    false
                //}
            }.invokeOnCompletion { isExecuted = it == null }
        }
    }

    private suspend fun iteratePaths(onFileExists: (suspend (name: String) -> String?)? = null) {
        if (requireDestinationPath && destinationPath == null) return
        if (files.isNullOrEmpty()) return
        logOperation()
        for (file in files) {
            if (requireFileExists && !file.exists()) continue
            fileAction(file, onFileExists)
        }
    }

    protected abstract suspend fun fileAction(
        file: File,
        onFileExists: (suspend (name: String) -> String?)? = null
    )

    protected open fun logOperation() {
        Log.d(
            javaClass.superclass?.simpleName ?: javaClass.simpleName,
            "executing ${javaClass.simpleName}..."
        )
    }

    protected fun File.ensurePathExists(): File {
        try {
            parentFile?.mkdirs()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return this
    }

}