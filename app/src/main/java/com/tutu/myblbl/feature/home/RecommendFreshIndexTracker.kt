package com.tutu.myblbl.feature.home

/**
 * Tracks the feed cursor used by the recommend API.
 *
 * The home recommend endpoint expects `fresh_idx` to keep moving forward.
 * Reusing `fresh_idx=1` on every page-1 reload returns the same anonymous feed,
 * which makes pull-to-refresh and tab reselect look like a no-op.
 *
 * Uses a timestamp-based seed so every cold start gets a different starting index,
 * preventing the API from returning stale results.
 */
internal class RecommendFreshIndexTracker {

    private val sequenceStartFreshIdx: Int
    private var highestResolvedFreshIdx: Int
    private var hasLoadedFirstPage = false

    init {
        val seed = (System.currentTimeMillis() / 1000).toInt()
        sequenceStartFreshIdx = seed
        highestResolvedFreshIdx = seed - 1
    }

    fun resolve(page: Int): Int {
        val normalizedPage = page.coerceAtLeast(1)
        val startIdx = if (normalizedPage == 1 && hasLoadedFirstPage) {
            highestResolvedFreshIdx + 1
        } else {
            sequenceStartFreshIdx
        }
        val freshIdx = startIdx + normalizedPage - 1
        highestResolvedFreshIdx = maxOf(highestResolvedFreshIdx, freshIdx)
        return freshIdx
    }

    fun markFirstPageLoaded() {
        hasLoadedFirstPage = true
    }
}
