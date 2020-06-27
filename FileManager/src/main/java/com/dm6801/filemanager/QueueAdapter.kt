package com.dm6801.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_queue.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QueueAdapter(private val operations: OperationsManager) :
    ListAdapter<QueueAdapter.SelectedItem, QueueAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<SelectedItem>() {
            override fun areItemsTheSame(oldItem: SelectedItem, newItem: SelectedItem): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: SelectedItem, newItem: SelectedItem): Boolean {
                return oldItem == newItem
            }
        }
    ) {

    private var recyclerView: RecyclerView? = null
    private var pathsAdapter: PathsAdapter? = null
    private var queueIndex = -1

    init {
        operations.observe(Observer { refresh() })
    }

    fun link(pathsAdapter: PathsAdapter?) {
        this.pathsAdapter = pathsAdapter
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    data class SelectedItem(
        val path: String
    )

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pathText: TextView? get() = itemView.item_queue_name
        private val button: ImageView? get() = itemView.item_queue_remove

        fun bind(item: SelectedItem) {
            pathText?.text = item.path
            button?.setOnClickListener { remove(adapterPosition) }
        }
    }

    fun submit(operation: Pair<Int, Operation>) {
        if (currentList.isNotEmpty()) return
        this.queueIndex = operation.first
        submitList(operation.second.files?.map { SelectedItem(it) }) {
            recyclerView?.isVisible = currentList.isNotEmpty()
        }
    }

    fun refresh() = CoroutineScope(Dispatchers.Main).launch {
        operations.get(queueIndex)?.let { submit(queueIndex to it) }
            ?: operations.get(0)?.let { submit(0 to it) }
            ?: submitList(null) {
                recyclerView?.isGone = true
            }
    }

    fun remove(position: Int) {
        if (position !in currentList.indices) return
        CoroutineScope(Dispatchers.IO).launch {
            val updated = currentList.toMutableList().apply { removeAt(position) }
            withContext(Dispatchers.Main) {
                submitList(updated) {
                    recyclerView?.isVisible = currentList.isNotEmpty()
                }
            }
            if (updated.isNullOrEmpty()) {
                operations.remove(queueIndex)
                queueIndex = -1
            } else {
                operations.update(queueIndex) {
                    apply { copy(files = files?.toMutableList()?.apply { removeAt(position) }) }
                }
            }
        }
    }

    /*fun clear() {
        if (queueIndex != -1) {
            submitList(null)
            val index = queueIndex
            queueIndex = -1
            operations.remove(index)
        }
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_queue, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}