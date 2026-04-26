package com.tutu.myblbl.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R
import com.tutu.myblbl.core.ui.base.BaseVideoAdapter
import com.tutu.myblbl.databinding.CellMovieBinding
import com.tutu.myblbl.model.lane.LaneItemModel
import com.tutu.myblbl.core.ui.image.ImageLoader
import com.tutu.myblbl.core.ui.focus.VideoCardFocusHelper

class LaneItemAdapter(
    private val onItemClick: (LaneItemModel) -> Unit = {},
    private val onItemFocusedView: ((View) -> Unit)? = null,
    private val onEdgeBottomDown: (() -> Boolean)? = null
) : BaseVideoAdapter<LaneItemModel, LaneItemAdapter.LaneItemViewHolder>() {

    init {
        setHasStableIds(true)
        setShowLoadMore(false)
    }

    override fun itemKey(item: LaneItemModel): String {
        return when {
            item.seasonId > 0L -> "season:${item.seasonId}"
            item.oid > 0L -> "oid:${item.oid}"
            item.linkValue != 0 -> "link:${item.linkType}:${item.linkValue}"
            item.link.isNotBlank() -> "url:${item.link}"
            item.cover.isNotBlank() && item.title.isNotBlank() -> "cover:${item.cover}|title:${item.title}"
            else -> "title:${item.title}|subtitle:${item.subTitle}"
        }
    }

    override fun areContentsSame(old: LaneItemModel, new: LaneItemModel): Boolean = old == new


    fun requestFirstItemFocus(recyclerView: RecyclerView): Boolean {
        val holder = recyclerView.findViewHolderForAdapterPosition(0) as? LaneItemViewHolder
        return holder?.requestFocus() == true
    }

    override fun onCreateContentViewHolder(parent: ViewGroup, viewType: Int): LaneItemViewHolder {
        val binding = CellMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LaneItemViewHolder(binding, onItemClick, onItemFocusedView, onEdgeBottomDown)
    }

    override fun onBindContentViewHolder(holder: LaneItemViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
    }

    override fun onViewRecycled(holder: LaneItemViewHolder) {
        holder.clearFocusState()
        super.onViewRecycled(holder)
    }

    class LaneItemViewHolder(
        private val binding: CellMovieBinding,
        private val onItemClick: (LaneItemModel) -> Unit,
        private val onItemFocused: ((View) -> Unit)?,
        private val onBottomEdgeDown: (() -> Boolean)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: LaneItemModel? = null

        init {
            binding.clickView.setOnClickListener {
                onItemFocused?.invoke(binding.clickView)
                currentItem?.let(onItemClick)
            }
            binding.clickView.setOnFocusChangeListener { view, hasFocus ->
                applyFocusState(hasFocus)
                if (hasFocus) {
                    onItemFocused?.invoke(view)
                }
            }
            VideoCardFocusHelper.bindSidebarExit(
                binding.clickView,
                onBottomEdgeDown = onBottomEdgeDown
            )
        }

        fun bind(item: LaneItemModel) {
            currentItem = item

            ImageLoader.loadSeriesCover(binding.imageView, item.cover)

            binding.textView.text = item.title

            val subtitle = item.desc.ifBlank { item.subTitle }
            if (subtitle.isBlank()) {
                binding.textSub.visibility = View.GONE
                binding.textSub.text = ""
            } else {
                binding.textSub.visibility = View.VISIBLE
                binding.textSub.text = subtitle
            }

            val badgeText = item.badgeInfo?.text.orEmpty()
            if (badgeText.isBlank()) {
                binding.textBadge.visibility = View.GONE
                binding.textBadge.text = ""
            } else {
                binding.textBadge.visibility = View.VISIBLE
                binding.textBadge.text = badgeText
                applyBadgeBackground(item.badgeInfo?.bgColorNight ?: item.badgeInfo?.bgColor)
            }

            applyFocusState(binding.clickView.hasFocus())
        }

        fun requestFocus(): Boolean = binding.clickView.requestFocus()

        fun clearFocusState() {
            applyFocusState(false)
        }

        private fun applyFocusState(hasFocus: Boolean) {
            binding.clickView.isSelected = hasFocus
            binding.textView.isSelected = hasFocus
        }

        private fun applyBadgeBackground(colorString: String?) {
            val context = binding.textBadge.context
            val drawable = AppCompatResources.getDrawable(
                context,
                R.drawable.badge_background
            )?.mutate() ?: return
            if (colorString.isNullOrBlank()) {
                binding.textBadge.background = drawable
                return
            }
            runCatching {
                val wrapped = DrawableCompat.wrap(drawable)
                DrawableCompat.setTint(wrapped, Color.parseColor(colorString))
                binding.textBadge.background = wrapped
            }.onFailure {
                binding.textBadge.background = drawable
            }
        }
    }
}
