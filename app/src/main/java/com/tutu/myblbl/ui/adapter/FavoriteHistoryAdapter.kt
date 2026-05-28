package com.tutu.myblbl.ui.adapter

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R
import com.tutu.myblbl.model.video.HistoryVideoModel
import com.tutu.myblbl.core.ui.image.ImageLoader
import com.tutu.myblbl.core.common.format.NumberUtils
import com.tutu.myblbl.core.common.log.VideoCardPerfLogger
import com.tutu.myblbl.core.common.time.TimeUtils
import com.tutu.myblbl.core.ui.focus.VideoCardFocusHelper
import com.tutu.myblbl.core.ui.focus.tv.TvFocusableAdapter
import com.tutu.myblbl.core.ui.video.VideoCardViews
import com.tutu.myblbl.core.ui.video.VideoLightCardFactory
import com.tutu.myblbl.ui.dialog.VideoCardMenuDialog
import java.util.concurrent.atomic.AtomicInteger

class FavoriteHistoryAdapter(
    private val onItemClick: (HistoryVideoModel) -> Unit,
    private val onItemFocused: ((Int) -> Unit)? = null,
    private val onItemFavoriteRemoved: ((HistoryVideoModel) -> Unit)? = null,
    private val onItemFocusedWithView: ((View, Int) -> Unit)? = null,
    private val onItemDpad: ((View, Int, KeyEvent) -> Boolean)? = null,
    private val onItemsChanged: (() -> Unit)? = null
) : ListAdapter<HistoryVideoModel, FavoriteHistoryAdapter.ViewHolder>(DiffCallback), TvFocusableAdapter {

    private var focusedPosition = RecyclerView.NO_POSITION
    private val contentViewType = nextViewType.getAndIncrement()

    init {
        setHasStableIds(true)
    }

    fun setData(newItems: List<HistoryVideoModel>) {
        val deduplicated = newItems.distinctBy(::favoriteHistoryItemKey)
        focusedPosition = focusedPosition
            .takeIf { it != RecyclerView.NO_POSITION && it < deduplicated.size }
            ?: RecyclerView.NO_POSITION
        submitList(deduplicated)
    }

    fun addData(newItems: List<HistoryVideoModel>) {
        val deduplicated = newItems
            .distinctBy(::favoriteHistoryItemKey)
            .filter { incoming ->
                currentList.none { existing ->
                    favoriteHistoryItemKey(existing) == favoriteHistoryItemKey(incoming)
                }
            }
        if (deduplicated.isEmpty()) return
        submitList(currentList + deduplicated)
    }

    fun getItemsSnapshot(): List<HistoryVideoModel> = currentList.toList()

    fun getFocusedPosition(): Int = focusedPosition

    override fun focusableItemCount(): Int = itemCount

    override fun stableKeyAt(position: Int): String? {
        return currentList.getOrNull(position)?.let(::favoriteHistoryItemKey)
    }

    override fun findPositionByStableKey(key: String): Int {
        return currentList.indexOfFirst { favoriteHistoryItemKey(it) == key }
            .takeIf { it >= 0 }
            ?: RecyclerView.NO_POSITION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val views = VideoCardPerfLogger.measureInflate("FavoriteHistoryAdapter.light") {
            VideoLightCardFactory.create(parent, source = "FavoriteHistoryAdapter.light")
        }
        return ViewHolder(
            views = views,
            onItemClick = onItemClick,
            onItemFocused = onItemFocused,
            updateFocusedPosition = { view, position ->
                val previous = focusedPosition
                focusedPosition = position
                if (previous != RecyclerView.NO_POSITION && previous != position) {
                    notifyItemChanged(previous)
                }
                setFocusedState(view, true)
                onItemFocusedWithView?.invoke(view, position)
            },
            clearFocusedPosition = { view ->
                setFocusedState(view, false)
            },
            onItemDisliked = { item -> removeDislikedItem(item) },
            onUpDisliked = { upName -> removeBlockedItems(upName) },
            onItemFavoriteRemoved = { item -> removeDislikedItem(item) },
            onItemDpad = onItemDpad,
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == focusedPosition)
    }

    override fun getItemViewType(position: Int): Int = contentViewType

    override fun getItemId(position: Int): Long = favoriteHistoryItemKey(getItem(position)).hashCode().toLong()

    private fun removeBlockedItems(blockedName: String) {
        val filtered = currentList.filter { !it.displayAuthorName.equals(blockedName, ignoreCase = true) }
        if (filtered.size == currentList.size) return
        submitList(filtered) {
            onItemsChanged?.invoke()
        }
    }

    private fun removeDislikedItem(item: HistoryVideoModel) {
        val key = favoriteHistoryItemKey(item)
        val filtered = currentList.filter { favoriteHistoryItemKey(it) != key }
        if (filtered.size == currentList.size) return
        submitList(filtered) {
            onItemsChanged?.invoke()
        }
    }

    class ViewHolder(
        private val views: VideoCardViews,
        private val onItemClick: (HistoryVideoModel) -> Unit,
        private val onItemFocused: ((Int) -> Unit)?,
        private val updateFocusedPosition: (View, Int) -> Unit,
        private val clearFocusedPosition: (View) -> Unit,
        private val onItemDisliked: ((HistoryVideoModel) -> Unit)? = null,
        private val onUpDisliked: ((String) -> Unit)? = null,
        private val onItemFavoriteRemoved: ((HistoryVideoModel) -> Unit)? = null,
        private val onItemDpad: ((View, Int, KeyEvent) -> Boolean)? = null
    ) : RecyclerView.ViewHolder(views.root) {

        private var currentItem: HistoryVideoModel? = null
        private val handler = Handler(Looper.getMainLooper())
        private var longPressRunnable: Runnable? = null
        private var longPressTriggered = false

        private val keyListener = View.OnKeyListener { view, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            startLongPress()
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        cancelLongPress()
                    }
                }
                false
            } else {
                onItemDpad?.invoke(view, keyCode, event) == true
            }
        }

        private fun showCardMenu() {
            cancelLongPress()
            val item = currentItem ?: return
            val video = item.toVideoModel()
            VideoCardMenuDialog(
                context = itemView.context,
                video = video,
                onDislikeVideo = { onItemDisliked?.invoke(item) },
                onDislikeUp = { upName -> onUpDisliked?.invoke(upName) },
                onFavoriteRemoved = { onItemFavoriteRemoved?.invoke(item) }
            ).show()
        }

        private fun startLongPress() {
            cancelLongPress()
            longPressTriggered = false
            longPressRunnable = Runnable {
                longPressTriggered = true
                showCardMenu()
            }
            handler.postDelayed(longPressRunnable!!, ViewConfiguration.getLongPressTimeout().toLong())
        }

        private fun cancelLongPress() {
            longPressRunnable?.let { handler.removeCallbacks(it) }
            longPressRunnable = null
        }

        init {
            views.imageView.clipToOutline = true
            views.imageView.outlineProvider = VideoAdapter.VideoViewHolder.coverOutlineProviderFor(views.imageView.resources)
            views.root.setOnClickListener {
                if (longPressTriggered) {
                    longPressTriggered = false
                    return@setOnClickListener
                }
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemFocused?.invoke(position)
                    updateFocusedPosition(views.root, position)
                }
                currentItem?.let(onItemClick)
            }
            @SuppressLint("ClickableViewAccessibility")
            views.root.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> startLongPress()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelLongPress()
                }
                false
            }
            views.root.setOnFocusChangeListener { _, hasFocus ->
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnFocusChangeListener
                }
                if (hasFocus) {
                    onItemFocused?.invoke(position)
                    updateFocusedPosition(views.root, position)
                } else {
                    clearFocusedPosition(views.root)
                }
            }
            VideoCardFocusHelper.bindSidebarExit(
                view = views.root,
                handleListDpadDown = false,
                chainedListener = keyListener
            )
            views.textLayer.setTitle("", lines = 1)
            views.textLayer.setOwner(ownerText = "", showAvatar = false, show = false)
            views.textLayer.clearHistoryTrailing()
            views.coverMetaOverlay.bind(
                showPlayCount = false,
                showDanmakuCount = false,
                durationText = "",
                showChargeBadge = false,
                showInteractionBadge = false
            )
        }

        fun bind(item: HistoryVideoModel, isFocused: Boolean) {
            currentItem = item
            views.root.isSelected = isFocused
            views.textLayer.isSelected = isFocused
            views.textLayer.setTitle(item.title.ifBlank { item.showTitle }, lines = 1)
            views.progressBar.visibility = View.GONE
            val authorName = item.displayAuthorName
            val hasAuthorName = authorName.isNotBlank()
            views.textLayer.setOwner(
                ownerText = formatFavoriteOwnerLine(item),
                showAvatar = hasAuthorName,
                badgeText = if (!hasAuthorName && item.isPortrait) "竖屏" else ""
            )
            views.textLayer.clearHistoryTrailing()

            val stat = item.cntInfo
            val durationValue = item.duration.coerceAtLeast(0L)
            val durationText = if (durationValue > 0L) NumberUtils.formatDuration(durationValue) else ""
            views.coverMetaOverlay.bind(
                playCountText = stat?.let { NumberUtils.formatCount(it.play) }.orEmpty(),
                showPlayCount = stat != null,
                danmakuText = stat?.let { NumberUtils.formatCount(it.danmaku) }.orEmpty(),
                showDanmakuCount = stat != null,
                durationText = durationText,
                showChargeBadge = item.isChargingExclusive,
                showInteractionBadge = item.isSteinsGate
            )

            ImageLoader.loadVideoCover(
                imageView = views.imageView,
                url = item.cover,
                deferUntilPreDraw = true
            )
        }

        private fun formatFavoriteOwnerLine(item: HistoryVideoModel): String {
            val favoriteTime = TimeUtils.formatRelativeTime(item.favTime)
                .takeIf { it.isNotBlank() }
                ?.let { "收藏于$it" }
                .orEmpty()
            return listOf(item.displayAuthorName, favoriteTime)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        setFocusedState(holder.itemView, false)
        super.onViewRecycled(holder)
    }

    private fun setFocusedState(view: View?, focused: Boolean) {
        view ?: return
        view.isSelected = focused
        view.findViewById<View>(R.id.textView)?.isSelected = focused
    }

    companion object {
        private val nextViewType = AtomicInteger(0x590100)

        private val DiffCallback = object : DiffUtil.ItemCallback<HistoryVideoModel>() {
            override fun areItemsTheSame(oldItem: HistoryVideoModel, newItem: HistoryVideoModel): Boolean {
                return favoriteHistoryItemKey(oldItem) == favoriteHistoryItemKey(newItem)
            }

            override fun areContentsTheSame(oldItem: HistoryVideoModel, newItem: HistoryVideoModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

private fun favoriteHistoryItemKey(item: HistoryVideoModel): String {
    return when {
        item.bvid.isNotBlank() -> "bvid:${item.bvid}"
        (item.history?.oid ?: 0L) > 0L -> "aid:${item.history?.oid}"
        else -> "title:${item.title}|cover:${item.cover}"
    }
}
