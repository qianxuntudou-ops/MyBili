package com.tutu.myblbl.ui.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.TypedValue
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R
import com.tutu.myblbl.databinding.CellSeriesFilterBinding
import com.tutu.myblbl.model.series.AllSeriesFilterModel
import com.tutu.myblbl.utils.AppLog

class AllSeriesFilterAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onItemFocused: (() -> Unit)? = null,
    private val onTopEdgeUp: (() -> Boolean)? = null,
    private val onLeftEdge: (() -> Boolean)? = null
) : RecyclerView.Adapter<AllSeriesFilterAdapter.FilterViewHolder>() {

    companion object {
        private const val TAG = "AllSeriesFocus"
    }

    private val items = mutableListOf<AllSeriesFilterModel>()
    private var expanded = false
    private var focusedPosition = RecyclerView.NO_POSITION
    private var pendingFocusPosition = RecyclerView.NO_POSITION
    var focusedView: android.view.View? = null
        private set

    fun setData(data: List<AllSeriesFilterModel>) {
        val oldItems = items.toList()
        val diffResult = DiffUtil.calculateDiff(FilterDiffCallback(oldItems, data))
        focusedView = null
        items.clear()
        items.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    fun requestFocusedView(): Boolean {
        val view = focusedView ?: return false
        return view.requestFocus()
    }

    fun requestSavedItemFocus(recyclerView: RecyclerView): Boolean {
        val position = focusedPosition
        AppLog.d(TAG, "adapter requestSavedItemFocus: position=$position itemCount=$itemCount")
        if (position == RecyclerView.NO_POSITION) {
            return false
        }
        return requestItemFocus(recyclerView, position)
    }

    fun requestItemFocus(recyclerView: RecyclerView, position: Int): Boolean {
        AppLog.d(TAG, "adapter requestItemFocus: position=$position itemCount=$itemCount")
        if (position !in 0 until itemCount) {
            return false
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder?.itemView?.requestFocus() == true) {
            pendingFocusPosition = RecyclerView.NO_POSITION
            AppLog.d(TAG, "adapter requestItemFocus immediate success: position=$position")
            return true
        }
        pendingFocusPosition = position
        AppLog.d(TAG, "adapter requestItemFocus deferred: position=$position")
        recyclerView.scrollToPosition(position)
        return true
    }

    fun setExpanded(expanded: Boolean) {
        if (this.expanded == expanded) {
            return
        }
        this.expanded = expanded
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = CellSeriesFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterViewHolder(binding, onItemClick, onTopEdgeUp) { view ->
            focusedView = view
            focusedPosition = holderBindingAdapterPosition(view)
            onItemFocused?.invoke()
        }.also { holder ->
            holder.onLeftEdge = onLeftEdge
        }
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(items[position], position, expanded)
        if (pendingFocusPosition == position) {
            AppLog.d(TAG, "adapter onBind pending focus: position=$position")
            holder.itemView.post {
                if (pendingFocusPosition != position) {
                    return@post
                }
                if (holder.itemView.requestFocus()) {
                    pendingFocusPosition = RecyclerView.NO_POSITION
                    AppLog.d(TAG, "adapter pending focus success: position=$position")
                } else {
                    AppLog.d(TAG, "adapter pending focus failed: position=$position")
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun holderBindingAdapterPosition(view: android.view.View): Int {
        return (view.parent as? RecyclerView)?.getChildAdapterPosition(view) ?: RecyclerView.NO_POSITION
    }

    private class FilterDiffCallback(
        private val oldItems: List<AllSeriesFilterModel>,
        private val newItems: List<AllSeriesFilterModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem.key == newItem.key && oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }

    class FilterViewHolder(
        private val binding: CellSeriesFilterBinding,
        private val onItemClick: (Int) -> Unit,
        private val onTopEdgeUp: (() -> Boolean)?,
        private val onFocused: (android.view.View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        var onLeftEdge: (() -> Boolean)? = null
        private val accentColor = TypedValue()
        private val defaultTextColor = TypedValue()

        init {
            binding.root.context.theme.resolveAttribute(
                android.R.attr.colorAccent,
                accentColor,
                true
            )
            binding.root.context.theme.resolveAttribute(
                android.R.attr.textColorPrimary,
                defaultTextColor,
                true
            )
        }

        fun bind(item: AllSeriesFilterModel, position: Int, expanded: Boolean) {
            binding.clickView.nextFocusUpId = if (position == 0) {
                R.id.button_back_1
            } else {
                View.NO_ID
            }
            if (item.iconResourceId != 0) {
                binding.imageIcon.visibility = View.VISIBLE
                binding.imageIcon.setImageResource(item.iconResourceId)
            } else {
                binding.imageIcon.setImageDrawable(null)
                binding.imageIcon.visibility = View.GONE
            }
            val hasSelectedOption = item.currentSelect > 0 && item.currentSelect < item.options.size
            binding.textFilter.text = if (hasSelectedOption) {
                item.options[item.currentSelect].title
            } else {
                item.title
            }
            val color = if (hasSelectedOption) accentColor.data else defaultTextColor.data
            binding.textFilter.setTextColor(color)
            binding.imageIcon.setColorFilter(color)
            binding.root.gravity = if (expanded) Gravity.CENTER_VERTICAL else Gravity.CENTER
            binding.textFilter.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.clickView.setOnClickListener {
                onItemClick(position)
            }
            binding.clickView.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    AppLog.d(TAG, "filter item focused: position=$bindingAdapterPosition")
                    onFocused(view)
                }
            }
            binding.clickView.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP &&
                    bindingAdapterPosition == 0
                ) {
                    onTopEdgeUp?.invoke() == true
                } else if (
                    event.action == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                ) {
                    onLeftEdge?.invoke() == true
                } else {
                    false
                }
            }
        }
    }
}
