package com.dm6801.filemanager

import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import java.io.File
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
            ?.execute(onFileExists = { fileExistsDialog(it).await() })
    }

    fun executeAll(path: String) {
        repeat(queue.size) {
            poll()?.apply { destinationPath = path }
                ?.execute(onFileExists = { fileExistsDialog(it).await() })
        }
    }

    fun openFile(path: String) {
        context.launchOpenFile(path)
    }

    private fun queue(
        operationType: Operation.Type,
        path: String,
        destination: String?
    ): Pair<Int, Operation> {
        return queue(operationType, listOf(path), destination)
    }

    private fun queue(
        operationType: Operation.Type,
        paths: List<String>,
        destination: String?
    ): Pair<Int, Operation> {
        val operation = Operation(operationType, paths, destination)
        val index = queue(operation)
        return (index to operation.observe())
    }

    private fun Operation.observe(): Operation = apply {
        observe(Observer { notifyObservers(Event.Execute) })
    }

    fun copy(paths: List<String>) {
        if (paths.isEmpty()) return
        queue(Operation.Type.Copy, paths, null)
    }

    fun copy(path: String) {
        if (path.isBlank()) return
        copy(listOf(path))
    }

    fun move(paths: List<String>) {
        if (paths.isEmpty()) return
        queue(Operation.Type.Move, paths, null)
    }

    fun move(path: String) {
        if (path.isBlank()) return
        move(listOf(path))
    }

    fun create(path: String) {
        val file = File(path)
        if (file.isDirectory) {
            createFileDialog(path) { name ->
                Operation(
                    Operation.Type.Create,
                    listOf("$path/${name ?: return@createFileDialog}"),
                    null
                ).observe()
                    .execute()
            }
        } else {
            Operation(Operation.Type.Create, listOf(path), null)
                .observe()
                .execute(onFileExists = { fileExistsDialog(it).await() })
        }
    }

    fun delete(path: String) {
        val file = File(path)
        if (!file.exists()) return
        deleteFileDialog(path) {
            Operation(Operation.Type.Delete, listOf(path), null)
                .observe()
                .execute()
        }
    }

    fun delete(paths: List<String>) {
        deleteFileDialog(paths.joinToString(", ")) {
            Operation(Operation.Type.Delete, paths, null)
                .observe()
                .execute()
        }
    }

    fun createFileDialog(path: String, action: (String?) -> Unit) {
        val editText = EditText(context)
        AlertDialog.Builder(context)
            .setView(editText)
            .setPositiveButton(R.string.dialog_file_create_positive_button, null)
            .setNegativeButton(R.string.dialog_file_create_negative_button) { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                        val name = editText.text?.toString() ?: return@setOnClickListener
                        if (name.isBlank()) return@setOnClickListener
                        val file = File(path, name)
                        if (file.exists()) {
                            Toast.makeText(context, R.string.toast_file_exists, Toast.LENGTH_SHORT)
                                .apply { setGravity(Gravity.CENTER, 0, 0) }
                                .show()
                            return@setOnClickListener
                        }
                        dismiss()
                        action(editText.text?.toString())
                    }
                }
            }
            .show()
    }

    fun deleteFileDialog(path: String, action: () -> Unit) {
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

    fun fileExistsDialog(name: String?): Deferred<String?> {
        val cont = CompletableDeferred<String?>()
        val editText = EditText(context).apply { setText(name) }
        AlertDialog.Builder(context)
            .setMessage(R.string.dialog_file_exists_message)
            .setView(editText)
            .setPositiveButton(R.string.dialog_file_exists_positive_button) { dialog, _ ->
                dialog.dismiss()
                cont.complete(editText.text?.toString())
            }
            .setNegativeButton(R.string.dialog_file_exists_negative_button) { dialog, _ ->
                dialog.cancel()
                cont.complete(null)
            }
            .show()
        return cont
    }

}