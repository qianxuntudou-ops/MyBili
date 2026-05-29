package com.tutu.myblbl.core.ui.base

import androidx.recyclerview.widget.RecyclerView

object VideoRecyclerViewTuning {
    private const val DEFAULT_ITEM_VIEW_CACHE_SIZE = 8
    private const val DEFAULT_MAX_RECYCLED_VIEWS = 60

    fun apply(
        recyclerView: RecyclerView,
        adapter: RecyclerView.Adapter<*>,
        itemViewCacheSize: Int = DEFAULT_ITEM_VIEW_CACHE_SIZE,
        pool: RecyclerView.RecycledViewPool = BaseListFragment.sharedVideoPool,
        maxRecycledViews: Int = DEFAULT_MAX_RECYCLED_VIEWS
    ) {
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(itemViewCacheSize)
        recyclerView.setRecycledViewPool(pool)
        val viewType = runCatching { adapter.getItemViewType(0) }.getOrNull() ?: return
        pool.setMaxRecycledViews(viewType, maxRecycledViews)
    }
}
