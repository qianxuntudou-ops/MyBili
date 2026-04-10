package com.tutu.myblbl.ui.fragment.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.R
import com.tutu.myblbl.databinding.FragmentBaseListBinding
import com.tutu.myblbl.network.NetworkManager
import com.tutu.myblbl.ui.adapter.VideoAdapter
import com.tutu.myblbl.ui.base.BaseFragment
import com.tutu.myblbl.ui.view.WrapContentGridLayoutManager
import com.tutu.myblbl.ui.widget.GridSpacingItemDecoration
import com.tutu.myblbl.utils.ContentFilter
import com.tutu.myblbl.utils.VideoRouteNavigator
import kotlinx.coroutines.launch

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
            runCatching {
                NetworkManager.apiService.getVideoByChannel(
                    channelId = tagId,
                    offset = offset,
                    pageSize = 30
                )
            }.onSuccess { response ->
                showLoading(false)
                isLoading = false
                if (response.isSuccess) {
                    val data = response.data
                    val list = data?.list ?: emptyList()
                    hasMore = data?.hasMore ?: false
                    offset = data?.offset ?: ""
                    videoAdapter.setShowLoadMore(hasMore)
                    val hasExistingItems = videoAdapter.getItem(0) != null
                    if (list.isEmpty() && !hasExistingItems) {
                        showEmpty()
                    } else {
                        showContent()
                        val videos = ContentFilter.filterVideos(requireContext(), list.map { it.toVideoModel() })
                        if (!hasExistingItems) {
                            videoAdapter.setData(videos)
                        } else {
                            videoAdapter.addData(videos)
                        }
                    }
                } else {
                    showError(response.message)
                }
            }.onFailure {
                showLoading(false)
                isLoading = false
                showError(it.message)
            }
        }
    }

    private fun loadMore() {
        if (isLoading || !hasMore) return
        loadData()
    }

    override fun onDestroyView() {
        videoAdapter.clear()
        recyclerView = null
        super.onDestroyView()
    }
}
