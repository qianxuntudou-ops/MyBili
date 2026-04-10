package com.tutu.myblbl.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R
import com.tutu.myblbl.databinding.CellUserBinding
import com.tutu.myblbl.model.user.FollowingModel
import com.tutu.myblbl.utils.ImageLoader

class FollowUserAdapter(
    private val onItemClick: (FollowingModel) -> Unit,
    private val onItemFocused: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<FollowUserAdapter.ViewHolder>() {

    private val items = mutableListOf<FollowingModel>()
    private var focusedPosition = RecyclerView.NO_POSITION

    fun setData(newItems: List<FollowingModel>) {
        val oldList = items.toList()
        val diffResult = DiffUtil.calculateDiff(FollowUserDiffCallback(oldList, newItems))
        items.clear()
        items.addAll(newItems)
        focusedPosition = focusedPosition
            .takeIf { it != RecyclerView.NO_POSITION && it < items.size }
            ?: if (items.isEmpty()) RecyclerView.NO_POSITION else 0
        diffResult.dispatchUpdatesTo(this)
    }

    fun addData(newItems: List<FollowingModel>) {
        val start = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position == focusedPosition)
    }

    override fun getItemCount(): Int = items.size

    fun getFocusedPosition(): Int = focusedPosition

    inner class ViewHolder(
        private val binding: CellUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }

            binding.root.setOnFocusChangeListener { _, hasFocus ->
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnFocusChangeListener
                }
                if (hasFocus) {
                    val oldPosition = focusedPosition
                    focusedPosition = position
                    onItemFocused?.invoke(position)
                    if (oldPosition != RecyclerView.NO_POSITION && oldPosition != position) {
                        notifyItemChanged(oldPosition)
                    }
                    notifyItemChanged(position)
                } else if (focusedPosition == position) {
                    notifyItemChanged(position)
                }
            }
        }

        fun bind(item: FollowingModel, isFocused: Boolean) {
            binding.root.isSelected = isFocused
            binding.textView.isSelected = isFocused
            binding.textView.text = item.uname
            binding.textSub.text = item.sign
            binding.textSub.isVisible = item.sign.isNotBlank()

            ImageLoader.loadCircle(
                imageView = binding.imageView,
                url = item.face,
                placeholder = R.drawable.default_avatar,
                error = R.drawable.default_avatar
            )
        }
    }

    private class FollowUserDiffCallback(
        private val oldList: List<FollowingModel>,
        private val newList: List<FollowingModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].mid == newList[newItemPosition].mid
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
