package com.dm6801.filemanager.operations

import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.lifecycle.Observer
import com.dm6801.filemanager.*
import com.dm6801.filemanager.safeLaunch
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.properties.Delegates

class OperationsManager(private val context: Context) {


    enum class Event {
        Queue, Update, Execute, Remove;
    }

    data class Update(
        val event: Event,
        val operation: Operation?
    )

    private val queue: List<Operation> = mutableListOf()
    val size: Int get() = queue.size
    private var observers: MutableList<Observer<Update>> = mutableListOf()
    private var onUpdate: Update? by Delegates.observable<Update?>(null) { _, _, newValue ->
        observers.forEach { it.onChanged(newValue) }
    }

    private val imm: InputMethodManager? get() = context.getSystemService()

    fun observe(observer: Observer<Update>): OperationsManager {
        observers.add(observer)
        return this
    }

    private fun Operation.notifyObservers(event: Event): Operation {
        onUpdate = Update(event, this)
        return this
    }

    fun queue(operation: Operation): Int {
        (queue as MutableList).add(operation)
        operation.notifyObservers(Event.Queue)
        return queue.lastIndex
    }

    fun peek(): Operation? = get(0)

    fun poll(): Operation? = remove(0)

    fun get(index: Int): Operation? = queue.getOrNull(index)

    fun update(index: Int, action: Operation.() -> Operation) {
        if (index in queue.indices) {
            val operation = queue[index].action()
            (queue as MutableList)[index] = operation
            operation.notifyObservers(Event.Update)
        }
    }

    fun remove(index: Int): Operation? {
        return if (index in queue.indices)
            (queue as MutableList).removeAt(index)
                .notifyObservers(Event.Remove)
        else null
    }

    fun clear() = (queue as MutableList).clear().also { onUpdate = null }

    fun executeNext(path: String) {
        poll()?.apply { destinationPath = path }
            ?.execute(observe = true, onFileExists = ::fileExistsDialog)
    }

    fun executeAll(path: String) {
        repeat(queue.size) {
            poll()?.apply { destinationPath = path }
                ?.execute(observe = true, onFileExists = ::fileExistsDialog)
        }
    }

    fun openFile(path: String) {
        context.launchOpenFile(path)
    }

    fun openFileWith(path: String) {
        context.launchOpenFilePicker(path)
    }

    fun copy(paths: List<String>) {
        if (paths.isEmpty()) return
        queue(Copy(paths))
    }

    fun copy(path: String) {
        if (path.isBlank()) return
        copy(listOf(path))
    }

    fun move(paths: List<String>) {
        if (paths.isEmpty()) return
        queue(Move(paths))
    }

    fun move(path: String) {
        if (path.isBlank()) return
        move(listOf(path))
    }

    fun createFile(path: String) {
        val file = File(path)
        if (file.isDirectory) {
            createFileDialog(path) { name ->
                if (name == null) return@createFileDialog
                Create(listOf("$path/$name"), Operation.Type.File)
                    .execute(observe = true)
            }
        } else {
            Create(listOf(path), Operation.Type.File)
                .execute(observe = true, onFileExists = ::fileExistsDialog)
        }
    }

    fun createFolder(path: String) {
        val file = File(path)
        if (file.isDirectory) {
            createFileDialog(
                path,
                R.string.toast_folder_exists
            ) { name ->
                if (name == null) return@createFileDialog
                Create(listOf("$path/$name"), Operation.Type.Folder)
                    .execute(observe = true)
            }
        } else {
            Create(listOf(path), Operation.Type.Folder)
                .execute(observe = true, onFileExists = ::fileExistsDialog)
        }
    }

    fun delete(path: String) {
        val file = File(path)
        if (!file.exists()) return
        deleteFileDialog(path) {
            Delete(listOf(path))
                .execute(observe = true)
        }
    }

    fun delete(paths: List<String>) {
        deleteFileDialog(paths.joinToString(", ")) {
            Delete(paths)
                .execute(observe = true)
        }
    }

    fun rename(source: String) {
        renameFileDialog(source) {
            Rename(source, it ?: return@renameFileDialog)
                .execute(observe = true)
        }
    }

    private fun Operation.observe(): Operation = apply {
        observe(Observer { notifyObservers(Event.Execute) })
    }

    private fun Operation.execute(
        observe: Boolean = false,
        onFileExists: suspend (name: String?) -> String? = { null }
    ) {
        if (observe) observe()
        execute(onFileExists)
    }

    private fun renameFileDialog(
        path: String,
        onExistMessage: Int = R.string.toast_file_exists,
        action: (String?) -> Unit
    ) {
        val editText = EditText(context).apply { setText(path.substringAfterLast("/")) }
        AlertDialog.Builder(context)
            .setView(editText)
            .setPositiveButton(R.string.dialog_file_rename_positive_button, null)
            .setNegativeButton(R.string.dialog_file_rename_negative_button) { dialog, _ -> dialog.cancel() }
            .create()
            .apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                        val name = editText.text?.toString()
                        if (name.isNullOrBlank()) return@setOnClickListener
                        val file = File(path, name)
                        if (file.exists()) {
                            Toast.makeText(context, onExistMessage, Toast.LENGTH_SHORT)
                                .apply { setGravity(Gravity.CENTER, 0, 0) }
                                .show()
                            return@setOnClickListener
                        }
                        dismiss()
                        action(editText.text?.toString())
                    }
                    editText.edit()
                }
            }
            .show()
    }

    private fun createFileDialog(
        path: String,
        onExistMessage: Int = R.string.toast_file_exists,
        action: (String?) -> Unit
    ) {
        val editText = EditText(context)
        AlertDialog.Builder(context)
            .setView(editText)
            .setPositiveButton(R.string.dialog_file_create_positive_button, null)
            .setNegativeButton(R.string.dialog_file_create_negative_button) { dialog, _ -> dialog.cancel() }
            .create()
            .apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                        val name = editText.text?.toString()
                        if (name.isNullOrBlank()) return@setOnClickListener
                        val file = File(path, name)
                        if (file.exists()) {
                            Toast.makeText(context, onExistMessage, Toast.LENGTH_SHORT)
                                .apply { setGravity(Gravity.CENTER, 0, 0) }
                                .show()
                            return@setOnClickListener
                        }
                        dismiss()
                        action(editText.text?.toString())
                    }
                    editText.edit()
                }
            }
            .show()
    }

    private fun deleteFileDialog(path: String, action: () -> Unit) {
        AlertDialog.Builder(context)
            .setMessage(
                context.getString(
                    R.string.dialog_file_delete_message,
                    path.substringAfterLast("/")
                )
            )
            .setPositiveButton(R.string.dialog_file_delete_positive_button) { dialog, _ ->
                dialog.dismiss()
                action()
            }
            .setNegativeButton(R.string.dialog_file_delete_negative_button) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private suspend fun fileExistsDialog(name: String?): String? = suspendCoroutine { cont ->
        CoroutineScope(Dispatchers.Main).safeLaunch {
            val editText = EditText(context).apply { setText(name) }
            AlertDialog.Builder(context)
                .setMessage(R.string.dialog_file_exists_message)
                .setView(editText)
                .setPositiveButton(R.string.dialog_file_exists_positive_button) { dialog, _ ->
                    dialog.dismiss()
                    cont.resume(editText.text?.toString())
                }
                .setNegativeButton(R.string.dialog_file_exists_negative_button) { dialog, _ ->
                    dialog.cancel()
                }
                .setOnCancelListener {
                    cont.resume(null)
                }
                .setOnDismissListener {
                    imm?.hideSoftInputFromWindow(
                        editText.windowToken,
                        InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
                }
                .create()
                .apply {
                    setOnShowListener {
                        editText.edit()
                    }
                }
                .show()
        }
    }

}