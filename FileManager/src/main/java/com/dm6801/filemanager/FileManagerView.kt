package com.dm6801.filemanager

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.android.synthetic.main.view_file_manager.view.*
import java.io.File

class FileManagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val pathsRecycler: RecyclerView? get() = file_manager_recycler
    private val queueRecycler: RecyclerView? get() = file_manager_queue_recycler
    private val clickArea: View? get() = file_manager_click_area
    private val menuButton: ImageView? get() = file_manager_menu_button
    private val menuView: LinearLayout? get() = file_manager_menu
    private val openButton: Button? get() = file_manager_menu_open
    private val deleteButton: Button? get() = file_manager_menu_delete
    private val folderButton: Button? get() = file_manager_menu_folder
    private val createButton: Button? get() = file_manager_menu_create
    private val copyButton: Button? get() = file_manager_menu_copy
    private val cutButton: Button? get() = file_manager_menu_cut
    private val moveButton: Button? get() = file_manager_menu_move
    private val unqueueButton: Button? get() = file_manager_menu_unqueue
    private val clearQueueButton: Button? get() = file_manager_menu_clear_queue
    private val deselectButton: Button? get() = file_manager_menu_deselect
    private val refreshButton: Button? get() = file_manager_menu_refresh

    private val operations = OperationsManager(context)
    private val pathsAdapter = PathsAdapter(operations).observe(Observer(::onSelected))
    private val queueAdapter = QueueAdapter(operations)

    init {
        inflate(context, R.layout.view_file_manager, this)
        linkAdapters()
    }

    fun openDirectory(file: File) {
        openDirectory(file.absolutePath)
    }

    fun openDirectory(path: String) {
        pathsAdapter.openDirectory(path)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initMenu()
        initRecyclerView()
        initQueueView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMenu() {
        clickArea?.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP)
                pathsAdapter.showPopupMenu(v, event.rawX to event.rawY)
            true
        }
        menuButton?.setOnClickListener {
            menuView?.run {
                val isVisible = !isVisible
                if (isVisible) refreshMenuButtons()
                this.isVisible = isVisible
            }
        }
        refreshMenuButtons()
        openButton?.setOnClickListener { pathsAdapter.openFile() }
        deleteButton?.setOnClickListener { pathsAdapter.deleteFiles() }
        createButton?.setOnClickListener { pathsAdapter.createFile() }
        folderButton?.setOnClickListener { pathsAdapter.createFolder() }
        copyButton?.setOnClickListener { pathsAdapter.copyFiles() }
        cutButton?.setOnClickListener { pathsAdapter.moveFiles() }
        moveButton?.setOnClickListener { pathsAdapter.executeQueue() }
        unqueueButton?.setOnClickListener { pathsAdapter.unqueue() }
        clearQueueButton?.setOnClickListener { pathsAdapter.clearQueue() }
        cutButton?.setOnClickListener { pathsAdapter.moveFiles() }
        deselectButton?.setOnClickListener { pathsAdapter.clearSelection() }
        refreshButton?.setOnClickListener {
            pathsAdapter.refresh()
            queueAdapter.refresh()
        }
    }

    private fun refreshMenuButtons(
        selectedSize: Int = pathsAdapter.selected.size,
        queueSize: Int = queueAdapter.itemCount
    ) {
        when {
            selectedSize <= 0 -> {
                openButton?.disable()
                deleteButton?.disable()
                createButton?.enable()
                copyButton?.disable()
                cutButton?.disable()
                deselectButton?.disable()
                refreshButton?.enable()
            }
            selectedSize == 1 -> {
                openButton?.enable()
                deleteButton?.enable()
                createButton?.enable()
                copyButton?.enable()
                cutButton?.enable()
                deselectButton?.enable()
                refreshButton?.enable()
            }
            selectedSize > 1 -> {
                openButton?.disable()
                deleteButton?.enable()
                createButton?.enable()
                copyButton?.enable()
                cutButton?.enable()
                deselectButton?.enable()
                refreshButton?.enable()
            }
        }
        when {
            queueSize <= 0 -> {
                moveButton?.disable()
                unqueueButton?.disable()
                clearQueueButton?.disable()
            }
            else -> {
                moveButton?.enable()
                unqueueButton?.enable()
                clearQueueButton?.enable()
            }
        }
    }

    private fun initRecyclerView() {
        pathsRecycler?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pathsAdapter
            itemAnimator = null
        }
    }

    private fun initQueueView() {
        queueRecycler?.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.HORIZONTAL)
            adapter = queueAdapter
            adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    refreshMenuButtons()
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    super.onItemRangeRemoved(positionStart, itemCount)
                    refreshMenuButtons()
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                    refreshMenuButtons()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    refreshMenuButtons()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    super.onItemRangeChanged(positionStart, itemCount)
                    refreshMenuButtons()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                    super.onItemRangeChanged(positionStart, itemCount, payload)
                    refreshMenuButtons()
                }
            })
        }
    }

    private fun linkAdapters() {
        pathsAdapter.link(queueAdapter)
        queueAdapter.link(pathsAdapter)
    }

    private fun onSelected(selected: List<PathsAdapter.Item>?) {
        refreshMenuButtons(selected?.size ?: 0)
    }

}