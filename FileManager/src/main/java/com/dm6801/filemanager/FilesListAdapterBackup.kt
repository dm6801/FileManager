package com.dm6801.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_directory.view.*
import kotlinx.android.synthetic.main.item_error.view.*
import kotlinx.android.synthetic.main.item_file.view.*
import java.io.File

class FilesListAdapterBackup :
    ListAdapter<FilesListAdapterBackup.Item, FilesListAdapterBackup.ViewHolder<FilesListAdapterBackup.Item>>(
        object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }) {

    sealed class Item(open val path: String) {
        class Back(path: String) : Item(path) {
            companion object {
                const val viewType = -1
            }

            val canRead: Boolean = File(path).canRead()
        }

        data class Entry(
            override val path: String,
            val type: FileType
        ) : Item(path) {
            constructor(file: File) : this(file.absolutePath, getType(file))
            constructor(path: String) : this(File(path))

            val file by lazy { File(path) }

            companion object {
                fun getType(file: File): FileType {
                    return try {
                        when {
                            file.isDirectory -> FileType.Directory
                            file.isFile -> FileType.File
                            !file.exists() -> FileType.NotExists
                            !file.canRead() -> FileType.NoAccess
                            else -> FileType.Error
                        }
                    } catch (e: SecurityException) {
                        FileType.NoAccess
                    } catch (t: Throwable) {
                        FileType.Error
                    }
                }
            }
        }
    }

    /*fun submitList(files: List<File>?, callback: (() -> Unit)? = null) {
        submitList(files?.map { Item.Entry(it) }, callback)
    }*/

    fun submitList(paths: List<String>?, backPath: String? = null, callback: (() -> Unit)? = null) {
        val list = mutableListOf<Item>()
        backPath?.let { Item.Back(it) }?.let(list::add)
        paths?.map { Item.Entry(it) }?.let(list::addAll)
        if (!list.isNullOrEmpty()) submitList(list, callback)
        else super.submitList(null, callback)
    }

    fun openDirectory(path: String) {
        val file = File(path)
        try {
            if (!file.isDirectory || !file.canRead()) return
            val back = path.substringBeforeLast("/")
            submitList(file.listFiles()?.map { it.absolutePath }, back)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is Item.Back -> Item.Back.viewType
            is Item.Entry -> item.type.ordinal
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<Item> {
        return when (viewType) {
            Item.Back.viewType ->
                BackViewHolder(inflate(parent, R.layout.item_back))
            FileType.Directory.ordinal ->
                DirectoryViewHolder(inflate(parent, R.layout.item_directory))
            FileType.File.ordinal ->
                FileViewHolder(inflate(parent, R.layout.item_file))
            else ->
                ErrorViewHolder(inflate(parent, R.layout.item_error))
        } as ViewHolder<Item>
    }

    private fun inflate(parent: ViewGroup, layout: Int): View {
        return LayoutInflater.from(parent.context).inflate(layout, parent, false)
    }

    override fun onBindViewHolder(holder: ViewHolder<Item>, position: Int) {
        if (position in 0 until itemCount) holder.bind(getItem(position))
    }

    abstract class ViewHolder<T : Item>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    inner class DirectoryViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val pathText: TextView? get() = itemView.item_directory_path
        override fun bind(item: Item.Entry) {
            pathText?.text = item.path
            itemView.setOnClickListener { openDirectory(item.path) }
        }
    }

    inner class FileViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val pathText: TextView? get() = itemView.item_file_path
        override fun bind(item: Item.Entry) {
            pathText?.text = item.path
        }
    }

    inner class ErrorViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val pathText: TextView? get() = itemView.item_error_path
        override fun bind(item: Item.Entry) {
            pathText?.text = item.path
        }
    }

    inner class BackViewHolder(itemView: View) : ViewHolder<Item.Back>(itemView) {
        override fun bind(item: Item.Back) {
            if (item.canRead)
                itemView.setOnClickListener { openDirectory(item.path) }
            else
                itemView.setBackgroundResource(R.color.gray_light)
        }
    }

}