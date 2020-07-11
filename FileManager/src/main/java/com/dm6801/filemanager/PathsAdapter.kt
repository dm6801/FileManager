package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.filemanager.operations.Copy
import com.dm6801.filemanager.operations.Move
import com.dm6801.filemanager.operations.OperationsManager
import kotlinx.android.synthetic.main.item_directory.view.*
import kotlinx.android.synthetic.main.item_error.view.*
import kotlinx.android.synthetic.main.item_file.view.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

class PathsAdapter(private val operations: OperationsManager) :
    ListAdapter<PathsAdapter.Item, PathsAdapter.ViewHolder<PathsAdapter.Item>>(diffUtil) {

    companion object {
        const val KEY_UPDATE_IS_SELECTED = "KEY_UPDATE_IS_SELECTED"
        private val diffUtil = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: Item, newItem: Item): Any? {
                return if (oldItem is Item.Entry &&
                    newItem is Item.Entry &&
                    oldItem.isSelected != newItem.isSelected
                ) newItem.path to (KEY_UPDATE_IS_SELECTED to newItem.isSelected)
                else super.getChangePayload(oldItem, newItem)
            }
        }
    }

    var rootPath: String? = null
    val selected get() = currentList.filter { it is Item.Entry && it.isSelected }
    private val isAnySelected: Boolean get() = currentList.any { it is Item.Entry && it.isSelected }

    private var observers: MutableList<Observer<List<Item>>> = mutableListOf()
    private var onSelectionUpdate: List<Item> by Delegates.observable(emptyList()) { _, _, newValue ->
        observers.forEach { it.onChanged(newValue) }
    }

    private var queueAdapter: QueueAdapter? = null

    init {
        operations.observe(Observer {
            when (it?.event) {
                OperationsManager.Event.Execute -> refresh()
                else -> {
                }
            }
        })
    }

    fun observe(observer: Observer<List<Item>>): PathsAdapter {
        observers.add(observer)
        return this
    }

    fun link(queueAdapter: QueueAdapter?) {
        this.queueAdapter = queueAdapter
    }

    fun openDirectory(path: String) {
        val file = File(path)
        try {
            if (!file.isDirectory || !file.canRead()) return
            this.rootPath = path
            submitList(file.listFiles()?.map { it.absolutePath })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    @Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
    fun submitList(paths: List<String>?, a: Boolean = false) {
        val list = paths?.map { Item.Entry(it) } ?: emptyList()
        submitList(listOf(backButtonDir()) + list)
    }

    private fun backButtonDir(): Item.Back {
        return rootPath?.substringBeforeLast("/")?.let { Item.Back(it) } ?: Item.Back("..")
    }

    fun refresh() = CoroutineScope(Dispatchers.Main).launch {
        submitList(listOf(backButtonDir())) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                rootPath?.let(::openDirectory)
            }
        }
    }

    fun select(x1: Int, y1: Int, x2: Int, y2: Int) {
        //recyclerView?.findChildViewUnder()
    }

    fun select(range: IntRange) {
        for (position in range) {
            (getItem(position) as? Item.Entry)?.isSelected = true
        }
        notifyItemRangeChanged(range.first, range.count())
        onSelectionUpdate = selected
    }

    fun select(position: Int) {
        (getItem(position) as? Item.Entry)?.isSelected = true
        notifyItemChanged(position)
        onSelectionUpdate = selected
    }

    fun deselect(position: Int) {
        (getItem(position) as? Item.Entry)?.isSelected = false
        notifyItemChanged(position)
        onSelectionUpdate = selected
    }

    fun clearSelection() = CoroutineScope(Dispatchers.IO).safeLaunch {
        val updated = currentList.map {
            if (it is Item.Entry) it.copy(isSelected = false)
            else it
        }
        withContext(Dispatchers.Main) {
            submitList(updated)
            onSelectionUpdate = selected
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        /*recyclerView.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP &&
                recyclerView.findChildViewUnder(event.rawX, event.rawY) == null
            ) {
                if (isAnySelected || operations.size != 0)
                    recyclerView.showPopupMenu(event.x to event.y)
            }
            false
        }*/
        _recyclerView = WeakReference(recyclerView)
    }

    private val recyclerView: RecyclerView? get() = _recyclerView.get()
    private var _recyclerView: WeakReference<RecyclerView?> = WeakReference(null)

    fun showPopupMenu(view: View, point: Pair<Float, Float>) {
        val anchor = View(view.context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            setBackgroundColor(Color.TRANSPARENT)
        }
        val parent = view.parent as? ViewGroup
        parent?.addView(anchor)
        anchor.x = point.first
        anchor.y = point.second
        PopupMenu(view.context, anchor)
            .apply {
                inflate(R.menu.actions)
                menu.findItem(R.id.menu_actions_paste).isVisible =
                    operations.peek()?.let { it is Copy || it is Move } == true
                menu.findItem(R.id.menu_actions_deselect).isVisible = isAnySelected
                menu.findItem(R.id.menu_actions_clear).isVisible = operations.size != 0
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_actions_paste -> {
                            executeQueue()
                            true
                        }
                        R.id.menu_actions_create -> {
                            createFile()
                            true
                        }
                        R.id.menu_actions_deselect -> {
                            clearSelection()
                            true
                        }
                        R.id.menu_actions_clear -> {
                            clearQueue()
                            true
                        }
                        R.id.menu_actions_refresh -> {
                            refresh()
                            true
                        }
                        else -> null
                    } ?: false
                }
                setOnDismissListener { parent?.removeView(anchor) }
                show()
            }
    }

    fun openFile() {
        operations.openFile(selected.firstOrNull()?.path ?: return)
    }

    fun createFile() {
        operations.createFile(rootPath ?: return)
    }

    fun createFolder() {
        operations.createFolder(rootPath ?: return)
    }

    fun deleteFiles() {
        operations.delete(selected.map { it.path })
    }

    fun copyFiles() {
        operations.copy(selected.map { it.path })
    }

    fun moveFiles() {
        operations.move(selected.map { it.path })
    }

    fun executeQueue() {
        operations.executeNext(rootPath ?: return)
    }

    fun unqueue() {
        operations.remove(0)
    }

    fun clearQueue() {
        operations.clear()
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

    override fun onBindViewHolder(
        holder: ViewHolder<Item>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.contains(KEY_UPDATE_IS_SELECTED))
            getItemTyped<Item.Entry>(position)
                ?.isSelected?.let { holder.isSelected = it }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Item> getItemTyped(position: Int): T? {
        return getItem(position) as? T
    }

    sealed class Item(open val path: String) {
        data class Back(override val path: String) : Item(path) {
            companion object {
                const val viewType = -1
            }

            val canRead: Boolean = File(path).canRead()
        }

        data class Entry(
            override val path: String,
            val type: FileType,
            var isSelected: Boolean
        ) : Item(path) {
            constructor(file: File) : this(file.absolutePath, getType(file), false)
            constructor(path: String) : this(File(path))

            val file by lazy { File(path) }
            val files: List<File>? = try {
                file.listFiles()?.toList()
            } catch (t: Throwable) {
                null
            }

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

    abstract inner class ViewHolder<T : Item>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var isSelected: Boolean = false

        open fun bind(item: T) {
            isSelected = item is Item.Entry && item.isSelected
            setBackground()
            setOnTouchListener(item)
        }

        protected open fun setBackground() {
            if (isSelected) itemView.setBackgroundResource(R.color.gray_light)
            else itemView.background = null
        }

        @SuppressLint("ClickableViewAccessibility")
        protected open fun setOnTouchListener(item: T) {
            val gestureDetector = GestureDetector(itemView.context, createGestureListener(item))
            itemView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
        }

        protected open fun createGestureListener(item: T): GestureListener {
            return GestureListener()
        }

        open inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                onLongClick()
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                onClick()
                return true
            }

            open fun onClick() {}
            open fun onLongClick() {
                toggleSelect(adapterPosition)
            }
        }

        private fun toggleSelect(adapterPosition: Int) {
            if (!isSelected) select(adapterPosition)
            else deselect(adapterPosition)
        }
    }

    inner class DirectoryViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val pathText: TextView? get() = itemView.item_directory_path
        private val descriptionText: TextView? get() = itemView.item_directory_description

        @SuppressLint("SetTextI18n")
        override fun bind(item: Item.Entry) {
            super.bind(item)
            pathText?.text = item.path
            val filesSize = item.files?.size ?: -1
            if (filesSize > 0) {
                descriptionText?.text = "       ${itemView.context.resources.getQuantityString(
                    R.plurals.files,
                    filesSize,
                    filesSize
                )}"
                descriptionText?.isVisible = true
            } else {
                descriptionText?.isGone = true
                descriptionText?.text = null
            }
        }

        override fun createGestureListener(item: Item.Entry): GestureListener {
            return object : GestureListener() {
                override fun onClick() {
                    super.onClick()
                    popupItemMenu(itemView, item)
                }

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    openDirectory(item.path)
                    return true
                }
            }
        }
    }

    inner class FileViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val pathText: TextView? get() = itemView.item_file_path
        override fun bind(item: Item.Entry) {
            super.bind(item)
            pathText?.text = item.path
        }

        override fun createGestureListener(item: Item.Entry): GestureListener {
            return object : GestureListener() {
                override fun onClick() {
                    super.onClick()
                    popupItemMenu(itemView, item)
                }

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    operations.openFile(item.path)
                    return true
                }
            }
        }
    }

    private fun popupItemMenu(itemView: View, item: Item.Entry) {
        PopupMenu(itemView.context, itemView)
            .apply {
                inflate(R.menu.item)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_file_open -> {
                            operations.openFile(item.path)
                            true
                        }
                        R.id.menu_file_copy -> {
                            operations.copy(item.path)
                            true
                        }
                        R.id.menu_file_move -> {
                            operations.move(item.path)
                            true
                        }
                        R.id.menu_file_delete -> {
                            operations.delete(item.path)
                            true
                        }
                        else -> null
                    } ?: false
                }
                show()
            }
    }

    inner class ErrorViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val pathText: TextView? get() = itemView.item_error_path
        override fun bind(item: Item.Entry) {
            super.bind(item)
            pathText?.text = item.path
        }
    }

    inner class BackViewHolder(itemView: View) : ViewHolder<Item.Back>(itemView) {
        override fun bind(item: Item.Back) {
            super.bind(item)
            if (item.canRead) itemView.background = null
            else itemView.setBackgroundResource(R.color.gray_light)
        }

        override fun setBackground() {}

        @SuppressLint("ClickableViewAccessibility")
        override fun setOnTouchListener(item: Item.Back) {
            if (item.canRead) super.setOnTouchListener(item)
            else itemView.setOnTouchListener { _, _ -> true }
        }

        override fun createGestureListener(item: Item.Back): GestureListener {
            return object : GestureListener() {
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    openDirectory(item.path)
                    return true
                }

                override fun onLongClick() {}
            }
        }
    }

}