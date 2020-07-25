package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.FileObserver
import android.util.Size
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.filemanager.operations.Copy
import com.dm6801.filemanager.operations.Move
import com.dm6801.filemanager.operations.OperationsManager
import kotlinx.android.synthetic.main.item_directory.view.*
import kotlinx.android.synthetic.main.item_error.view.*
import kotlinx.android.synthetic.main.item_file.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.any
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.none
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.sortedWith
import kotlin.collections.toList
import kotlin.properties.Delegates

class PathsAdapter(private val operations: OperationsManager) :
    ListAdapter<PathsAdapter.Item, PathsAdapter.ViewHolder<PathsAdapter.Item>>(diffUtil) {

    companion object {
        const val KEY_UPDATE_IS_SELECTED = "KEY_UPDATE_IS_SELECTED"
        private val SEPARATOR = File.separatorChar
        private const val THUMBNAIL_WIDTH = 128
        private const val THUMBNAIL_HEIGHT = 128

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
                ) KEY_UPDATE_IS_SELECTED
                else super.getChangePayload(oldItem, newItem)
            }
        }
    }

    var rootPath: String? = null; private set
    val selected get() = currentList.filter { it is Item.Entry && it.isSelected }
    private val isAnySelected: Boolean get() = currentList.any { it is Item.Entry && it.isSelected }

    private var selectionObservers: MutableList<Observer<List<Item>>> = mutableListOf()
    private var onSelectionUpdate: List<Item> by Delegates.observable(emptyList()) { _, _, newValue ->
        selectionObservers.forEach { it.onChanged(newValue) }
    }

    private var pathObservers: MutableList<Observer<String?>> = mutableListOf()
    private var onPathChanged: String? by Delegates.observable<String?>(null) { _, _, newValue ->
        pathObservers.forEach { it.onChanged(newValue) }
    }

    private var queueAdapter: QueueAdapter? = null

    private val bitmapCache: MutableMap<String, Bitmap> = mutableMapOf()
    private var displayFullPath: Boolean = false

    /*init {
        operations.observe(Observer {
            when (it?.event) {
                OperationsManager.Event.Execute -> refresh()
                else -> {
                }
            }
        })
    }*/

    fun onSelected(observer: Observer<List<Item>>): PathsAdapter {
        selectionObservers.add(observer)
        return this
    }

    fun onPathChanged(observer: Observer<String?>): PathsAdapter {
        pathObservers.add(observer)
        return this
    }

    fun link(queueAdapter: QueueAdapter?) {
        this.queueAdapter = queueAdapter
    }

    private var fileObserver: FileObserver? = null

    fun openDirectory(path: String) {
        val file = File(path)
        try {
            if (!file.isDirectory || !file.canRead()) return
            this.rootPath = path
            observeDirectory(path)
            CoroutineScope(Dispatchers.IO).launch {
                if (bitmapCache.size >= 30) launch { clearCache() }
                val files = file.listFiles()
                val sorted = files
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.absolutePath }))
                    ?.map { it.absolutePath }
                submitList(sorted) {
                    onPathChanged = path
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun observeDirectory(path: String) {
        fileObserver?.stopWatching()
        fileObserver = FileObserver(path) { event, _ ->
            when (event and FileObserver.ALL_EVENTS) {
                FileObserver.CREATE,
                FileObserver.DELETE,
                FileObserver.MOVED_FROM,
                FileObserver.MOVED_TO -> refresh()
            }
        }
        fileObserver?.startWatching()
    }

    @Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
    fun submitList(paths: List<String>?, a: Boolean = false, callback: () -> Unit) {
        val list = paths?.map { Item.Entry(it) } ?: emptyList()
        submitList(listOf(backButtonDir()) + list, callback)
    }

    private fun backButtonDir(): Item.Back {
        return rootPath?.substringBeforeLast("/")
            ?.let { Item.Back(it) }
            ?: Item.Back("..")
    }

    fun refresh() {
        rootPath?.let(::openDirectory)
    }

    private fun getVisibleChildrenRange(): IntRange? {
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return null
        return layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()
    }

    fun select(rect: Rect) {
        val childrenPositions = getVisibleChildrenRange()
        val children = childrenPositions
            ?.mapNotNull { recyclerView?.findViewHolderForAdapterPosition(it)?.itemView }
            ?: emptyList()
        var first = -1
        var last = -1
        var index = 0
        for (child in children) {
            val viewHitRect = child.hitRect()
            if (first == -1) {
                if (viewHitRect.intersect(rect)) first = index
            } else {
                if (viewHitRect.intersect(rect)) last = index
                else break
            }
            index += 1
        }
        if (first == -1) {
            clearSelection()
            return
        }
        if (last == -1) last = first
        if (last < first) {
            clearSelection()
            return
        }
        select(first..last)
    }

    private fun select(range: IntRange) {
        val listRange = 0 until currentList.size
        if (range.none { it in listRange }) return
        for (position in listRange) {
            getItemTyped<Item.Entry>(position)?.run {
                isSelected = position in range
                notifyItemChanged(position, KEY_UPDATE_IS_SELECTED)
            }
        }
        onSelectionUpdate = selected
    }

    fun select(position: Int) {
        getItemTyped<Item.Entry>(position)?.run {
            isSelected = true
            notifyItemChanged(position, KEY_UPDATE_IS_SELECTED)
        }
        onSelectionUpdate = selected
    }

    fun deselect(position: Int) {
        getItemTyped<Item.Entry>(position)?.run {
            isSelected = false
            notifyItemChanged(position, KEY_UPDATE_IS_SELECTED)
        }
        onSelectionUpdate = selected
    }

    fun clearSelection() {
        if (selected.isEmpty()) return
        currentList.forEach { (it as? Item.Entry)?.isSelected = false }
        notifyItemRangeChanged(0, currentList.size, KEY_UPDATE_IS_SELECTED)
        onSelectionUpdate = emptyList()
    }

    private val recyclerView: RecyclerView? get() = _recyclerView.get()
    private var _recyclerView: WeakReference<RecyclerView?> = WeakReference(null)

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        _recyclerView = WeakReference(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        clearCache()
    }

    override fun onViewRecycled(holder: ViewHolder<Item>) {
        super.onViewRecycled(holder)
        (holder as? FileViewHolder)?.recycle()
    }

    private fun clearCache() {
        val values = bitmapCache.values
        bitmapCache.clear()
        values.forEach(Bitmap::recycle)
    }

    fun showPopupMenu(view: View, point: Pair<Float, Float>) {
        val anchor = View(view.context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            setBackgroundColor(Color.TRANSPARENT)
        }
        val parent = view.parent as? ViewGroup
        parent?.addView(anchor)
        anchor.x = point.first
        anchor.y = point.second
        PopupMenu(view.context, anchor).apply {
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

    fun openFile(path: String? = selected.firstOrNull()?.path) {
        val file = File(path ?: return)
        when {
            file.isDirectory -> openDirectory(path)
            file.isFile -> operations.openFile(path)
        }
    }

    fun openFileWith(path: String? = selected.firstOrNull()?.path) {
        val file = File(path ?: return)
        if (file.isFile) operations.openFileWith(path)
    }

    fun rename() {
        operations.rename(selected.firstOrNull()?.path ?: return)
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
            Item.Back.viewType -> BackViewHolder(inflate(parent, R.layout.item_back))
            FileType.Directory.ordinal ->
                DirectoryViewHolder(inflate(parent, R.layout.item_directory))
            FileType.File.ordinal -> FileViewHolder(inflate(parent, R.layout.item_file))
            else -> ErrorViewHolder(inflate(parent, R.layout.item_error))
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
        if (payloads.contains(KEY_UPDATE_IS_SELECTED)) {
            getItemTyped<Item.Entry>(position)?.let { holder.update(it.isSelected) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Item> getItemTyped(position: Int): T? {
        if (position !in 0 until currentList.size) return null
        return try {
            val item = getItem(position)// as? T
            if (T::class.java.isAssignableFrom(item::class.java)) return item as T
            else null
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
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
            val files: List<File>? =
                try {
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
        private var isSelected: Boolean = false

        open fun bind(item: T) {
            isSelected = item is Item.Entry && item.isSelected
            setBackground()
            setOnTouchListener(item)
        }

        open fun update(isSelected: Boolean) {
            this.isSelected = isSelected
            setBackground()
        }

        protected open fun setBackground() {
            if (isSelected) itemView.setBackgroundResource(R.color.gray_light)
            else itemView.background = null
        }

        @SuppressLint("ClickableViewAccessibility")
        protected open fun setOnTouchListener(item: T) {
            itemView.setOnTouchListener(TouchListener())
        }

        open fun onClick() {}

        open fun onDoubleClick() {}

        open fun onLongClick() {
            toggleSelect(adapterPosition)
        }

        inner class TouchListener : com.dm6801.filemanager.TouchListener() {
            override fun onSingleClick(v: View?, event: MotionEvent): Boolean {
                onClick()
                return super.onSingleClick(v, event)
            }

            override fun onDoubleClick(v: View?, event: MotionEvent): Boolean {
                onDoubleClick()
                return true
            }

            override fun onLongClick(v: View?, event: MotionEvent): Boolean {
                onLongClick()
                return true
            }
        }

        private fun toggleSelect(adapterPosition: Int) {
            if (!isSelected) select(adapterPosition)
            else deselect(adapterPosition)
        }
    }

    inner class DirectoryViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val imageView: ImageView? get() = itemView.item_directory_icon
        private val pathText: TextView? get() = itemView.item_directory_path
        private val descriptionText: TextView? get() = itemView.item_directory_description

        override fun bind(item: Item.Entry) {
            super.bind(item)
            imageView?.setImageResource(R.drawable.folder)
            setText(item.path, item.files)
        }

        @SuppressLint("SetTextI18n")
        private fun setText(path: String, files: List<File>?) {
            pathText?.text =
                if (displayFullPath) path
                else path.substringAfterLast(SEPARATOR)
            val numberOfFiles = files?.size ?: -1
            if (numberOfFiles > 0) {
                descriptionText?.text = "       ${itemView.context.resources.getQuantityString(
                    R.plurals.files,
                    numberOfFiles,
                    numberOfFiles
                )}"
                descriptionText?.isVisible = true
            } else {
                descriptionText?.isGone = true
                descriptionText?.text = null
            }
        }

        override fun onClick() {
            super.onClick()
            popupItemMenu(itemView, getItemTyped(adapterPosition) ?: return)
        }

        override fun onDoubleClick() {
            super.onDoubleClick()
            openDirectory(getItemTyped<Item.Entry>(adapterPosition)?.path ?: return)
        }
    }

    inner class FileViewHolder(itemView: View) : ViewHolder<Item.Entry>(itemView) {
        private val imageView: ImageView? get() = itemView.item_file_icon
        private val pathText: TextView? get() = itemView.item_file_path

        override fun bind(item: Item.Entry) {
            super.bind(item)
            setText(item.path)
            setImage(item.path)
        }

        private fun setText(path: String) {
            pathText?.text =
                if (displayFullPath) path
                else path.substringAfterLast(SEPARATOR)
        }

        private fun setImage(path: String) {
            bitmapCache[path]?.run {
                imageView?.setImageBitmap(this)
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                val mimeType = getMimeType(path, recyclerView?.context)
                val file = File(path)
                val thumbnail = when {
                    mimeType != null -> extractThumbnail(path, mimeType)
                    file.isImage() -> extractThumbnail(path, "image/*")
                    file.isVideo() -> extractThumbnail(path, "video/*")
                    file.isAudio() -> extractThumbnail(path, "audio/*")
                    else -> null
                }
                withContext(Dispatchers.Main) {
                    thumbnail?.let { imageView?.setImageBitmap(it) }
                        ?: imageView?.setImageResource(R.drawable.file)
                }
            }
        }

        @Suppress("DEPRECATION")
        private suspend fun extractThumbnail(path: String, mimeType: String): Bitmap? {
            return if (mimeType.startsWith("image") ||
                mimeType.startsWith("video") ||
                mimeType.startsWith("audio")
            ) withContext(Dispatchers.IO) {
                ThumbnailUtils.extractThumbnail(
                    File(path),
                    mimeType,
                    Size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                )?.also { bitmapCache[path] = it }
            }
            else null
        }

        override fun onClick() {
            super.onClick()
            popupItemMenu(itemView, getItemTyped(adapterPosition) ?: return)
        }

        override fun onDoubleClick() {
            super.onDoubleClick()
            operations.openFile(getItemTyped<Item.Entry>(adapterPosition)?.path ?: return)
        }

        fun recycle() {
            imageView?.setImageDrawable(null)
        }
    }

    private var menu: PopupMenu? = null

    private fun popupItemMenu(itemView: View, item: Item.Entry) {
        if (menu != null) return
        menu?.menu?.close()
        menu = PopupMenu(itemView.context, itemView).apply {
            inflate(R.menu.item)
            menu.findItem(R.id.menu_file_open_with)?.isVisible = item.file.isFile
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_file_open -> {
                        openFile(item.path)
                        true
                    }
                    R.id.menu_file_open_with -> {
                        openFileWith(item.path)
                        true
                    }
                    R.id.menu_file_rename -> {
                        operations.rename(item.path)
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
            setOnDismissListener {
                this@PathsAdapter.menu = null
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

        override fun onDoubleClick() {
            super.onDoubleClick()
            openDirectory(getItemTyped<Item.Back>(adapterPosition)?.path ?: return)
        }

        override fun onLongClick() {}
    }

}