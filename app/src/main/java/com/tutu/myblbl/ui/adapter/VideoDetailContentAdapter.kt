package com.tutu.myblbl.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.tutu.myblbl.R
import com.tutu.myblbl.core.ui.image.ImageLoader
import com.tutu.myblbl.core.ui.system.ScreenUtils
import com.tutu.myblbl.core.common.format.NumberUtils
import com.tutu.myblbl.core.common.time.TimeUtils
import com.tutu.myblbl.databinding.CellSeriesLaneBinding
import com.tutu.myblbl.databinding.CellVideoBinding
import com.tutu.myblbl.databinding.CellVideoDetailHeadBinding
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.model.video.detail.Tag
import com.tutu.myblbl.model.video.detail.VideoView
import java.util.Locale

class VideoDetailContentAdapter(
    private val onPlayClick: () -> Unit,
    private val onUploaderClick: () -> Unit,
    private val onLikeClick: () -> Unit,
    private val onCoinClick: () -> Unit,
    private val onFavoriteClick: () -> Unit,
    private val onTagClick: (Tag) -> Unit,
    private val onPageClick: (VideoModel) -> Unit,
    private val onUgcEpisodeClick: (VideoModel) -> Unit,
    private val onUgcOrderToggle: () -> Unit,
    private val onRelatedVideoClick: (VideoModel) -> Unit
) : ListAdapter<VideoDetailContentAdapter.Row, RecyclerView.ViewHolder>(RowItemCallback) {

    sealed interface Row {
        data class Header(
            val view: VideoView,
            val tags: List<Tag>,
            val isLiked: Boolean,
            val isCoined: Boolean,
            val isFavorited: Boolean
        ) : Row

        data class Pages(val items: List<VideoModel>) : Row

        data class UgcSeason(
            val title: String,
            val items: List<VideoModel>,
            val isReverse: Boolean
        ) : Row

        data class Related(val items: List<VideoModel>) : Row
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Row.Header -> VIEW_TYPE_HEADER
            is Row.Pages -> VIEW_TYPE_PAGES
            is Row.UgcSeason -> VIEW_TYPE_UGC_SEASON
            is Row.Related -> VIEW_TYPE_RELATED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> VideoDetailHeadViewHolder(
                CellVideoDetailHeadBinding.inflate(inflater, parent, false),
                onPlayClick,
                onUploaderClick,
                onLikeClick,
                onCoinClick,
                onFavoriteClick,
                onTagClick,
                parent.context
            )

            VIEW_TYPE_PAGES -> PagesLaneViewHolder(
                CellSeriesLaneBinding.inflate(inflater, parent, false),
                onPageClick
            )

            VIEW_TYPE_UGC_SEASON -> UgcSeasonLaneViewHolder(
                CellSeriesLaneBinding.inflate(inflater, parent, false),
                onUgcEpisodeClick,
                onUgcOrderToggle
            )

            else -> RelatedLaneViewHolder(
                CellSeriesLaneBinding.inflate(inflater, parent, false),
                onRelatedVideoClick
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is Row.Header -> (holder as VideoDetailHeadViewHolder).bind(row.view, row.tags, row.isLiked, row.isCoined, row.isFavorited)
            is Row.Pages -> (holder as PagesLaneViewHolder).bind(row.items)
            is Row.UgcSeason -> (holder as UgcSeasonLaneViewHolder).bind(row.title, row.items, row.isReverse)
            is Row.Related -> (holder as RelatedLaneViewHolder).bind(row.items)
        }
    }

    class VideoDetailHeadViewHolder(
        private val binding: CellVideoDetailHeadBinding,
        private val onPlayClick: () -> Unit,
        private val onUploaderClick: () -> Unit,
        private val onLikeClick: () -> Unit,
        private val onCoinClick: () -> Unit,
        private val onFavoriteClick: () -> Unit,
        private val onTagClick: (Tag) -> Unit,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTags: List<Tag> = emptyList()

        init {
            binding.buttonPlay.setOnClickListener { onPlayClick() }
            binding.buttonUploader.setOnClickListener { onUploaderClick() }
            binding.buttonLike.setOnClickListener { onLikeClick() }
            binding.buttonCoin.setOnClickListener { onCoinClick() }
            binding.buttonFavorite.setOnClickListener { onFavoriteClick() }
        }

        fun bind(view: VideoView, tags: List<Tag>, isLiked: Boolean, isCoined: Boolean, isFavorited: Boolean) {
            ImageLoader.loadVideoCover(
                imageView = binding.imageCover,
                url = view.pic,
                placeholder = R.drawable.default_video,
                error = R.drawable.default_video
            )

            binding.textTitle.text = view.title

            val stat = view.stat
            val subtitleText = buildString {
                if (view.pubDate > 0) {
                    append("发布于：")
                    append(TimeUtils.formatTime(view.pubDate))
                }
                stat?.let { s ->
                    if (isNotEmpty()) append(" · ")
                    append(formatCount(s.view))
                    append("播放")
                    append(" · ")
                    append(formatCount(s.danmaku))
                    append("弹幕")
                    if (s.like > 0) {
                        append(" · ")
                        append(formatCount(s.like))
                        append("点赞")
                    }
                    if (s.coin > 0) {
                        append(" · ")
                        append(formatCount(s.coin))
                        append("投币")
                    }
                    if (s.favorite > 0) {
                        append(" · ")
                        append(formatCount(s.favorite))
                        append("收藏")
                    }
                }
            }
            binding.textSubtitle.text = subtitleText

            val owner = view.owner
            if (owner != null) {
                binding.textName.text = owner.name
                ImageLoader.loadCircle(
                    imageView = binding.imageAvatar,
                    url = owner.face,
                    placeholder = R.drawable.default_avatar,
                    error = R.drawable.default_avatar
                )
                binding.imageAvatar.setBadge(
                    officialVerifyType = owner.officialVerify?.type ?: -1
                )
            }

            binding.textDescription.text = view.desc

            updateTagLayout(tags)
            updateActionButtons(isLiked, isCoined, isFavorited)
        }

        private fun updateTagLayout(tags: List<Tag>) {
            binding.viewFlexLayout.removeAllViews()
            if (tags.isEmpty()) {
                binding.viewFlexLayout.visibility = View.GONE
                return
            }
            currentTags = tags
            binding.viewFlexLayout.visibility = View.VISIBLE
            tags.take(6).forEach { tag ->
                val tagView = createTagView(tag.tagName)
                tagView.setOnClickListener { onTagClick(tag) }
                binding.viewFlexLayout.addView(tagView)
            }
        }

        private fun createTagView(text: String): AppCompatTextView {
            val textSizePx = context.resources.getDimension(R.dimen.px30)
            val ta = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
            val textColor = ta.getColor(0, 0)
            ta.recycle()
            return AppCompatTextView(context).apply {
                this.text = text
                tag = text
                setTextSize(0, textSizePx)
                setTextColor(textColor)
                setBackgroundResource(R.drawable.button_common)
                setPadding(20, 5, 20, 5)
                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isClickable = true
                isFocusable = true
            }
        }

        private fun updateActionButtons(isLiked: Boolean, isCoined: Boolean, isFavorited: Boolean) {
            val pink = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.pink)
            )
            val default = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.textColor)
            )
            binding.buttonLike.imageTintList = if (isLiked) pink else default
            binding.buttonCoin.imageTintList = if (isCoined) pink else default
            binding.buttonFavorite.imageTintList = if (isFavorited) pink else default
        }

        private fun formatCount(count: Long): String {
            return when {
                count >= 100000000 -> String.format(Locale.getDefault(), "%.1f亿", count / 100000000.0)
                count >= 10000 -> String.format(Locale.getDefault(), "%.1f万", count / 10000.0)
                count >= 1000 -> String.format(Locale.getDefault(), "%.1f千", count / 1000.0)
                else -> count.toString()
            }
        }
    }

    class PagesLaneViewHolder(
        private val binding: CellSeriesLaneBinding,
        onPageClick: (VideoModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val adapter = EpisodeListAdapter()

        init {
            adapter.setShowLoadMore(false)
            binding.recyclerView.layoutManager =
                LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.isFocusable = false
            binding.buttonOrder.visibility = View.GONE
            adapter.setOnItemClickListener { _, item ->
                onPageClick(item)
            }
        }

        fun bind(items: List<VideoModel>) {
            binding.topTitle.text = binding.root.context.getString(
                R.string.video_detail_pages_title, items.size
            )
            adapter.setData(items)
        }
    }

    class UgcSeasonLaneViewHolder(
        private val binding: CellSeriesLaneBinding,
        onEpisodeClick: (VideoModel) -> Unit,
        private val onOrderToggle: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val videoAdapter = VideoAdapter(itemWidthPx = ScreenUtils.getScreenWidth(binding.root.context) / 5)

        init {
            videoAdapter.setShowLoadMore(false)
            binding.recyclerView.layoutManager =
                LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
            binding.recyclerView.adapter = videoAdapter
            binding.recyclerView.isFocusable = false
            binding.buttonOrder.visibility = View.VISIBLE
            binding.buttonOrder.setOnClickListener {
                onOrderToggle()
            }
            videoAdapter.setOnItemClickListener { _, item ->
                onEpisodeClick(item)
            }
        }

        fun bind(title: String, items: List<VideoModel>, isReverse: Boolean) {
            binding.topTitle.text = title
            binding.textOrder.text = if (isReverse) "正序" else "倒序"
            videoAdapter.setData(items)
        }
    }

    class RelatedLaneViewHolder(
        private val binding: CellSeriesLaneBinding,
        onVideoClick: (VideoModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val videoAdapter = VideoAdapter(itemWidthPx = ScreenUtils.getScreenWidth(binding.root.context) / 5)

        init {
            videoAdapter.setShowLoadMore(false)
            binding.recyclerView.layoutManager =
                LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
            binding.recyclerView.adapter = videoAdapter
            binding.recyclerView.isFocusable = false
            binding.buttonOrder.visibility = View.GONE
            videoAdapter.setOnItemClickListener { _, item ->
                onVideoClick(item)
            }
        }

        fun bind(items: List<VideoModel>) {
            binding.topTitle.text = binding.root.context.getString(R.string.related_video)
            videoAdapter.setData(items)
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PAGES = 1
        private const val VIEW_TYPE_UGC_SEASON = 2
        private const val VIEW_TYPE_RELATED = 3

        private val RowItemCallback = object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(oldItem: Row, newItem: Row): Boolean {
                return stableId(oldItem) == stableId(newItem)
            }

            override fun areContentsTheSame(oldItem: Row, newItem: Row): Boolean {
                return oldItem == newItem
            }

            private fun stableId(row: Row): String {
                return when (row) {
                    is Row.Header -> "header"
                    is Row.Pages -> "pages"
                    is Row.UgcSeason -> "ugc_season:${row.title}"
                    is Row.Related -> "related"
                }
            }
        }
    }
}
