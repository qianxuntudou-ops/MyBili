package com.tutu.myblbl.feature.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R
import com.tutu.myblbl.databinding.FragmentBaseListBinding
import com.tutu.myblbl.repository.VideoRepository
import com.tutu.myblbl.ui.adapter.VideoAdapter
import com.tutu.myblbl.core.ui.base.BaseFragment
import com.tutu.myblbl.core.ui.layout.WrapContentGridLayoutManager
import com.tutu.myblbl.core.ui.decoration.GridSpacingItemDecoration
import com.tutu.myblbl.core.common.content.ContentFilter
import com.tutu.myblbl.core.ui.focus.RecyclerViewLoadMoreFocusController
import com.tutu.myblbl.core.navigation.VideoRouteNavigator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ChannelVideoFragment : BaseFragment<FragmentBaseListBinding>() {

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_TAG_ID = "tagId"

        fun newInstance(title: String, tagId: Long): ChannelVideoFragment {
            return ChannelVideoFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_TAG_ID to tagId
                )
            }
        }
    }

    private var title: String = ""
    private var tagId: Long = 0L
    private var offset: String = ""
    private var hasMore = true
    private var isLoading = false
    private var recyclerView: RecyclerView? = null
    private val videoAdapter = VideoAdapter()
    private var loadMoreFocusController: RecyclerViewLoadMoreFocusController? = null
    private val videoRepository: VideoRepository by inject()

    override fun initArguments() {
        title = arguments?.getString(ARG_TITLE).orEmpty()
        tagId = arguments?.getLong(ARG_TAG_ID) ?: 0L
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBaseListBinding {
        return FragmentBaseListBinding.inflate(inflater, container!!)
    }

    override fun isTopBarVisible(): Boolean = true

    override fun initView() {
        setMainTitle(title)
        videoAdapter.setShowLoadMore(false)

        val spanCount = 4
        val layoutManager = WrapContentGridLayoutManager(requireContext(), spanCount)
        val decoration = GridSpacingItemDecoration(spanCount, resources.getDimensionPixelSize(R.dimen.px20), true)
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = layoutManager
        recyclerView?.adapter = videoAdapter
        recyclerView?.addItemDecoration(decoration)
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                if (lastVisiblePosition >= totalItemCount - 12) {
                    loadMore()
                }
            }
        })
        installLoadMoreFocusController()

        videoAdapter.setOnItemClickListener { _, item ->
            VideoRouteNavigator.openVideo(
                context = requireContext(),
                video = item,
                playQueue = com.tutu.myblbl.ui.activity.PlayerActivity.buildPlayQueue(
                    videoAdapter.getItemsSnapshot(),
                    item
                )
            )
        }
    }

    override fun initData() {
        loadData()
    }

    private fun loadData() {
        if (isLoading || tagId <= 0) return
        isLoading = true
        showLoading(true)
        lifecycleScope.launch {
            videoRepository.getChannelVideos(
                channelId = tagId,
                offset = offset,
                pageSize = 30
            ).onSuccess { page ->
                showLoading(false)
                isLoading = false
                hasMore = page.hasMore
                offset = page.offset
                videoAdapter.setShowLoadMore(hasMore)
                val hasExistingItems = videoAdapter.getItem(0) != null
                if (page.videos.isEmpty() && !hasExistingItems) {
                    showEmpty()
                } else {
                    showContent()
                    val videos = ContentFilter.filterVideos(
                        requireContext(),
                        page.videos
                    )
                    if (!hasExistingItems) {
                        videoAdapter.setData(videos)
                    } else {
                        videoAdapter.addData(videos)
                    }
                    loadMoreFocusController?.consumePendingFocusAfterLoadMore()
                }
            }.onFailure {
                showLoading(false)
                isLoading = false
                loadMoreFocusController?.clearPendingFocusAfterLoadMore()
                showError(it.message)
            }
        }
    }

    private fun loadMore() {
        if (isLoading || !hasMore) return
        loadData()
    }

    private fun installLoadMoreFocusController() {
        val rv = recyclerView ?: return
        loadMoreFocusController?.release()
        loadMoreFocusController = RecyclerViewLoadMoreFocusController(
            recyclerView = rv,
            callbacks = object : RecyclerViewLoadMoreFocusController.Callbacks {
                override fun canLoadMore(): Boolean = !isLoading && hasMore

                override fun loadMore() {
                    if (!canLoadMore()) {
                        return
                    }
                    loadMore()
                }
            }
        ).also { it.install() }
    }

    override fun onDestroyView() {
        loadMoreFocusController?.release()
        loadMoreFocusController = null
        videoAdapter.clear()
        recyclerView = null
        super.onDestroyView()
    }
}
