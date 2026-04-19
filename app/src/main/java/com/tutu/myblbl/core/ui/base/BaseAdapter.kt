package com.tutu.myblbl.core.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R

abstract class BaseAdapter<MODEL, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    internal val items = ArrayList<MODEL>()
    internal var showLoadMore = true
    internal var focusedView: View? = null
    internal var rememberedPosition: Int = RecyclerView.NO_POSITION

    protected open fun areItemsSame(old: MODEL, new: MODEL): Boolean = old == new
    protected open fun areContentsSame(old: MODEL, new: MODEL): Boolean = old == new

    fun contentCount(): Int = items.size

    fun getRememberedPosition(): Int = rememberedPosition

    fun rememberItemInteraction(view: View, position: Int) {
        if (position == RecyclerView.NO_POSITION) {
            return
        }
        focusedView = view
        rememberedPosition = position
    }

    fun setShowLoadMore(show: Boolean) {
        if (showLoadMore == show) {
            return
        }
        showLoadMore = show
        if (show) {
            notifyItemInserted(items.size)
        } else {
            notifyItemRemoved(items.size)
        }
    }

    fun addAll(list: List<MODEL>) {
        val size = items.size
        items.addAll(list)
        if (list.size == 1) {
            notifyItemInserted(size)
        } else {
            notifyItemRangeInserted(size, list.size)
        }
    }

    fun getItem(position: Int): MODEL? {
        return if (position in items.indices) items[position] else null
    }

    fun getItemsSnapshot(): List<MODEL> = items.toList()

    abstract fun onCreateContentViewHolder(parent: ViewGroup, viewType: Int): VH

    fun setData(data: List<MODEL>) {
        focusedView = null
        rememberedPosition = rememberedPosition
            .takeIf { it != RecyclerView.NO_POSITION && it < data.size }
            ?: RecyclerView.NO_POSITION
        val oldItems = items.toList()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = data.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                areItemsSame(oldItems[oldPos], data[newPos])
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                areContentsSame(oldItems[oldPos], data[newPos])
        })
        items.clear()
        items.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    fun clear() {
        focusedView = null
        rememberedPosition = RecyclerView.NO_POSITION
        items.clear()
    }

    override fun getItemCount(): Int {
        return if (showLoadMore && items.isNotEmpty()) items.size + 1 else items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size && showLoadMore && items.isNotEmpty()) {
            LOAD_MORE_TYPE
        } else {
            super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        if (viewType == LOAD_MORE_TYPE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_load_more, parent, false)
            @Suppress("UNCHECKED_CAST")
            return LoadMoreViewHolder(view) as VH
        }
        return onCreateContentViewHolder(parent, viewType)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) {
        if (position == items.size && showLoadMore && items.isNotEmpty()) {
            return
        }
        onBindContentViewHolder(holder, position)
    }

    abstract fun onBindContentViewHolder(holder: VH, position: Int)

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        const val LOAD_MORE_TYPE = -1000
    }
}
