package com.tutu.myblbl.feature.player.douyin

import android.content.Context
import com.tutu.myblbl.core.common.content.ContentFilter
import com.tutu.myblbl.core.common.log.AppLog
import com.tutu.myblbl.core.common.settings.AppSettingsDataStore
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.network.api.ApiService
import com.tutu.myblbl.network.cookie.CookieManager

class DouyinModeManager(
    private val appContext: Context,
    private val appSettings: AppSettingsDataStore,
    private val apiService: ApiService,
    private val noCookieApiService: ApiService,
    private val cookieManager: CookieManager
) {

    companion object {
        private const val TAG = "DouyinQueue"
        const val KEY_DOUYIN_MODE = "douyin_mode"
        const val INITIAL_LOAD_COUNT = 10
        const val APPEND_LOAD_COUNT = 10
        const val HISTORY_WINDOW_SIZE = 5
        const val FUTURE_MIN_BUFFER_SIZE = 15
        const val MAX_APPEND_PAGES_PER_RUN = 3
    }

    private val recommendList = mutableListOf<VideoModel>()
    private var currentIndex: Int = -1
    private var sourceAid: Long = 0L
    private var initialized: Boolean = false
    private var appending: Boolean = false
    private var freshIdx: Int = 0
    private var generation: Long = 0L

    /** 读取设置存储，判断抖音模式是否已启用 */
    fun isEnabled(): Boolean {
        return appSettings.getCachedString(KEY_DOUYIN_MODE) == "开"
    }

    /** 检查给定视频是否符合抖音模式条件 */
    fun isApplicable(video: VideoModel?): Boolean {
        if (video == null) return false
        if (!isEnabled()) return false
        return isDouyinPlayable(video)
    }

    /** 检查给定视频是否是抖音队列允许播放的普通视频 */
    fun isDouyinPlayable(video: VideoModel?): Boolean {
        if (video == null) return false
        if (video.isPgc) return false
        if (video.isLive) return false
        return video.hasPlaybackIdentity
    }

    /** 初始化推荐列表，当前视频作为起点，后续仅调用首页推荐 API */
    suspend fun initialize(sourceVideo: VideoModel) {
        if (!isDouyinPlayable(sourceVideo)) {
            reset()
            return
        }
        if (initialized && sourceAid == sourceVideo.aid) {
            return
        }
        reset()
        sourceAid = sourceVideo.aid
        freshIdx = nextRecommendStartFreshIdx()
        recommendList.add(sourceVideo)
        currentIndex = 0
        initialized = true
        AppLog.i(TAG, "initialize source=${sourceVideo.queueId()} startFreshIdx=$freshIdx")
        appending = true
        try {
            fillFutureBuffer("initialize", INITIAL_LOAD_COUNT, MAX_APPEND_PAGES_PER_RUN)
        } finally {
            appending = false
        }
    }

    /** 返回下一个推荐视频，到达末尾返回 null */
    fun next(): VideoModel? {
        if (!initialized || recommendList.isEmpty()) {
            return null
        }
        if (currentIndex >= recommendList.lastIndex) {
            return null
        }
        currentIndex++
        return recommendList[currentIndex]
    }

    /** 预览下一个视频（不移动索引），用于预加载 */
    fun peekNext(): VideoModel? {
        if (!initialized || recommendList.isEmpty()) return null
        val nextIndex = currentIndex + 1
        if (nextIndex > recommendList.lastIndex) return null
        return recommendList[nextIndex]
    }

    /** 预览上一个视频（不移动索引），用于手势预览 */
    fun peekPrevious(): VideoModel? {
        if (!initialized || recommendList.isEmpty()) return null
        val previousIndex = currentIndex - 1
        if (previousIndex < 0) return null
        return recommendList[previousIndex]
    }

    /** 返回上一个推荐视频，到达开头返回 null */
    fun previous(): VideoModel? {
        if (!initialized || recommendList.isEmpty()) {
            return null
        }
        if (currentIndex <= 0) {
            return null
        }
        currentIndex--
        return recommendList[currentIndex]
    }

    /** 列表是否已初始化且非空 */
    fun hasList(): Boolean = initialized && recommendList.isNotEmpty()

    /** 当前索引是否达到追加阈值 */
    fun shouldAppend(): Boolean {
        return initialized &&
            !appending &&
            futureCount() < FUTURE_MIN_BUFFER_SIZE
    }

    /** 重置所有状态 */
    fun reset() {
        generation++
        recommendList.clear()
        currentIndex = -1
        sourceAid = 0L
        initialized = false
        appending = false
        freshIdx = 0
    }

    /** 追加更多推荐视频，调用首页推荐 API 获取下一页 */
    suspend fun appendMore() {
        if (!initialized || appending) {
            return
        }
        if (!shouldAppend()) {
            return
        }
        appending = true
        try {
            fillFutureBuffer("append", APPEND_LOAD_COUNT, MAX_APPEND_PAGES_PER_RUN)
        } finally {
            appending = false
        }
    }

    private suspend fun fillFutureBuffer(reason: String, pageSize: Int, maxPages: Int) {
        val generationAtStart = generation
        var pagesFetched = 0
        var totalFetched = 0
        var totalAdded = 0

        while (
            pagesFetched < maxPages &&
            futureCount() < FUTURE_MIN_BUFFER_SIZE
        ) {
            val pageFreshIdx = freshIdx
            val more = fetchRecommend(pageFreshIdx, pageSize)
            if (generationAtStart != generation) {
                return
            }

            pagesFetched++
            freshIdx++
            totalFetched += more.size

            val toAdd = more
                .filterNot { candidate -> recommendList.any { it.isSameVideo(candidate) } }
            recommendList.addAll(toAdd)
            totalAdded += toAdd.size
            trimWindow()
        }

        AppLog.i(
            TAG,
            "fill_done reason=$reason pages=$pagesFetched fetched=$totalFetched added=$totalAdded " +
                "index=$currentIndex size=${recommendList.size} future=${futureCount()} nextFreshIdx=$freshIdx"
        )
    }

    private suspend fun fetchRecommend(idx: Int, count: Int): List<VideoModel> {
        return try {
            val loggedIn = cookieManager.hasSessionCookie()
            val requestIdx = idx.coerceAtLeast(1)
            AppLog.i(TAG, "recommend_request freshIdx=$requestIdx count=$count loggedIn=$loggedIn")
            val response = requestRecommend(
                api = if (loggedIn) apiService else noCookieApiService,
                idx = requestIdx,
                count = count
            )
            val resolvedResponse = if (loggedIn && response.code == -101) {
                AppLog.w(TAG, "recommend auth expired, fallback noCookie freshIdx=$requestIdx count=$count")
                requestRecommend(noCookieApiService, requestIdx, count)
            } else {
                response
            }
            if (resolvedResponse.isSuccess) {
                val rawItems = resolvedResponse.data?.items.orEmpty()
                val playableItems = rawItems.filter {
                    isDouyinPlayable(it)
                }
                val filteredItems = ContentFilter.filterVideos(appContext, playableItems)
                AppLog.i(
                    TAG,
                    "recommend_result freshIdx=$requestIdx raw=${rawItems.size} playable=${playableItems.size} " +
                        "filtered=${filteredItems.size} first=${filteredItems.take(3).joinToString("|") { it.queueId() }}"
                )
                filteredItems
            } else {
                AppLog.w(TAG, "recommend failed code=${resolvedResponse.code} message=${resolvedResponse.errorMessage} loggedIn=$loggedIn")
                emptyList()
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "recommend exception loggedIn=${cookieManager.hasSessionCookie()} freshIdx=$idx count=$count message=${e.message}")
            emptyList()
        }
    }

    private suspend fun requestRecommend(
        api: ApiService,
        idx: Int,
        count: Int
    ) = api.getRecommendList(
        freshIdx = idx,
        ps = count
    )

    private fun nextRecommendStartFreshIdx(): Int {
        val seconds = System.currentTimeMillis() / 1000L
        return (seconds + generation % 1000L)
            .coerceIn(1L, Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun trimWindow() {
        if (!initialized || recommendList.isEmpty()) return

        val removeBefore = (currentIndex - HISTORY_WINDOW_SIZE).coerceAtLeast(0)
        if (removeBefore > 0) {
            repeat(removeBefore) { recommendList.removeAt(0) }
            currentIndex -= removeBefore
        }

    }

    private fun futureCount(): Int {
        if (!initialized || recommendList.isEmpty()) return 0
        return (recommendList.lastIndex - currentIndex).coerceAtLeast(0)
    }

    private fun VideoModel.isSameVideo(other: VideoModel): Boolean {
        return when {
            aid > 0L && other.aid > 0L -> aid == other.aid
            bvid.isNotBlank() && other.bvid.isNotBlank() -> bvid == other.bvid
            else -> false
        }
    }

    private fun VideoModel.queueId(): String {
        return "aid=$aid,bvid=$bvid,cid=$cid,title=${title.take(16)}"
    }
}
