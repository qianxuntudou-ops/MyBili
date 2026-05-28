package com.tutu.myblbl.feature.player.view

import android.os.SystemClock
import com.tutu.myblbl.core.common.log.AppLog
import com.tutu.myblbl.model.dm.DmMaskRepository
import com.tutu.myblbl.model.dm.DmMaskTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 弹幕防挡蒙版控制器（clipOutPath 方案）。
 *
 * 只保留：
 *  - anchor 接入（视频帧 PTS ↔ wall clock）
 *  - seek 状态机
 *  - currentVideoPtsMs() 一个公式
 *  - 后台段预解析（preloadThread）
 */
class DmMaskController(
    private val maskHostProvider: () -> DanmakuMaskHostLayout?,
    private var repository: DmMaskRepository
) {
    companion object {
        private const val TAG = "DmMaskController"
        private const val SEEK_READY_STABILIZE_MS = 80L
        private const val SEEK_RECOVER_HARD_TIMEOUT_MS = 1500L

        /**
         * **mask 比 video 多走的上屏延迟**（1 vsync ≈ 16-17ms@60Hz）。
         *
         * 物理推导：
         *  - **video** 走 SurfaceView → SurfaceFlinger 直接合成，从 anchor.releaseTimeNs 到屏幕约 1 vsync
         *  - **mask** 走 View dispatchDraw → 主线程同步到 RenderThread → SurfaceFlinger 合成，约 2 vsync
         *  - 差 = 1 vsync ≈ 16ms
         *
         * 不补这 16ms：mask 永远滞后 video 1 帧。人物横移 300px/s 时 → 5 像素错位，
         * 帧率越高画面运动越剧烈，肉眼越明显。
         *
         * 注意这是**真补偿**，不是当年那套"50ms audio-video + maskDt/2 + coverage/2 ≈ 80ms"的过度 lookahead。
         * 32ms 覆盖 View 绘制链路和蒙版帧量化误差，避免人物移动时遮罩持续落后一小段。
         */
        private const val MASK_LOOKAHEAD_MS = 32L

        /** 同步诊断日志节流间隔（每秒最多 1 条）。 */
        private const val DIAG_LOG_INTERVAL_MS = 1000L
    }

    private var enabled = false
    private var currentCid: Long = 0L
    private var maskReady = false
    private var currentTimeline: DmMaskTimeline? = null

    private var isPlaying: Boolean = false
    private var playbackReady: Boolean = false

    @Volatile
    private var playbackSpeed: Float = 1.0f

    @Volatile
    private var hasVideoAnchor: Boolean = false
    @Volatile
    private var anchorPresentationTimeUs: Long = 0L
    @Volatile
    private var anchorReleaseTimeNs: Long = 0L

    private var awaitingSeekReady: Boolean = false
    private var seekReadyAt: Long = 0L
    private var seekHardDeadlineMs: Long = 0L

    private var lastPreloadedSegIndex: Int = -1

    /**
     * 当前正在处理的预解析段索引（用于去重，避免 anchor 60Hz 重复 launch 协程）。
     * -1 表示空闲。仅在主/playback 线程读写——anchor 路径用 @Volatile 隔离。
     */
    @Volatile
    private var preloadingSegIndex: Int = -1

    /** 上次输出同步诊断的时刻（节流用）。 */
    @Volatile
    private var lastDiagLogMs: Long = 0L

    var playerPositionProvider: (() -> Long)? = null

    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setEnabled(enabled: Boolean) {
        AppLog.d(TAG, "setEnabled: $enabled, maskReady=$maskReady")
        if (this.enabled == enabled) return
        this.enabled = enabled
        if (!enabled) {
            clearMask()
        } else if (maskReady) {
            invalidateMaskHost()
        }
    }

    suspend fun loadMask(maskUrl: String, cid: Long, fps: Int): Boolean {
        AppLog.d(TAG, "loadMask: cid=$cid, fps=$fps, enabled=$enabled")
        currentCid = cid
        maskReady = false
        lastPreloadedSegIndex = -1
        clearMask()

        val data = repository.downloadAndParse(maskUrl, cid, fps)
        maskReady = data != null
        if (!maskReady) {
            AppLog.d(TAG, "Mask load failed for cid=$cid")
        } else {
            val timeline = repository.getTimeline(cid)
            currentTimeline = timeline
            AppLog.d(TAG, "Mask loaded OK: segments=${data?.rawSegments?.size}, timeline=${timeline != null}")
            maskHostProvider()?.let { host ->
                host.timeline = timeline
            }
            if (enabled) invalidateMaskHost()
            // 后台预解析前几段
            preloadAhead(0)
        }
        return maskReady
    }

    fun pushMaskUpdate() {
        if (!enabled || !maskReady || currentCid <= 0L) return
        if (shouldSkipForSeek()) {
            clearMask()
            return
        }
        // 检查当前段是否需要预解析下一段
        checkAndPreloadNext()
        invalidateMaskHost()
    }

    fun currentVideoPtsMs(): Long {
        if (!hasVideoAnchor) {
            val pos = playerPositionProvider?.invoke() ?: 0L
            return pos
        }
        val nowNs = System.nanoTime()
        val anchorPtsMs = anchorPresentationTimeUs / 1000L
        val pts = if (!isPlaying) {
            // 暂停时 ExoPlayer 不再 release 新视频帧，anchor 冻结。继续按 wall clock 外推会让 mask
            // 「飘」过静止的人物——直接用最近 anchor 的 PTS（屏幕此时显示的就是该 PTS）。
            anchorPtsMs
        } else {
            val deltaNs = ((nowNs - anchorReleaseTimeNs) * playbackSpeed).toLong()
            anchorPtsMs + deltaNs / 1_000_000L + maskLookaheadMs
        }
        return pts
    }

    /**
     * 由 [DanmakuMaskHostLayout.dispatchDraw] 在每次成功查到 mask frame 后调用。
     * 当 frame 引用变化或每秒采样窗口到达时输出一条诊断，让对齐偏差具体到数字：
     *
     *  - `query`：mask 用来查 timeline 的 PTS（含 lookahead）
     *  - `frame.pts`：timeline 返回的 mask frame 实际 PTS（presentationTimeMs）
     *  - `frame-query`：query 与实际 frame.pts 的偏差，± maskDt/2 是正常 round-to-nearest 量纲
     *  - `anchor.pts`：最近 video frame anchor 的 PTS
     *  - `anchor.age`：anchor 距 now 的 wall clock 间隔（越大说明 anchor 推送越落后）
     *  - `anchor.interval(ema)`：anchor 推送的 EMA 间隔（video 帧率倒数，<= 50ms 正常）
     *  - `playerPos`：ExoPlayer.currentPosition（master clock）
     *  - `lookahead`：当前 [maskLookaheadMs]
     */
    fun reportFrameQuery(queryPtsMs: Long, framePtsMs: Long) {
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastDiagLogMs < DIAG_LOG_INTERVAL_MS) return
        lastDiagLogMs = nowMs
        val playerPos = playerPositionProvider?.invoke() ?: -1L
        val anchorPtsMs = if (hasVideoAnchor) anchorPresentationTimeUs / 1000L else -1L
        val nowNs = System.nanoTime()
        val anchorAgeMs = if (hasVideoAnchor) (nowNs - anchorReleaseTimeNs) / 1_000_000L else -1L
        val frameMinusQuery = framePtsMs - queryPtsMs
        AppLog.d(
            TAG,
            "pts diag: query=${queryPtsMs}ms frame.pts=${framePtsMs}ms frame-query=${frameMinusQuery}ms " +
                "anchor.pts=${anchorPtsMs}ms anchor.age=${anchorAgeMs}ms anchor.interval(ema)=${anchorIntervalMsEma}ms " +
                "playerPos=${playerPos}ms speed=$playbackSpeed playing=$isPlaying lookahead=${maskLookaheadMs}ms"
        )
    }

    /**
     * 让 lookahead 可在运行时动态调整（默认 [MASK_LOOKAHEAD_MS]）。
     * 调试用：把这个 setter 暴露到设置面板或 adb 命令上，实测调参不用重新编译。
     */
    fun setMaskLookaheadMs(value: Long) {
        maskLookaheadMs = value.coerceIn(-100L, 200L)
        AppLog.d(TAG, "maskLookahead → ${maskLookaheadMs}ms")
    }

    @Volatile
    private var maskLookaheadMs: Long = MASK_LOOKAHEAD_MS

    /**
     * 供 [DanmakuMaskHostLayout.shouldRenderMask] 调用：mask 数据未就绪、用户关闭、
     * 或 seek 等待视频首帧的窗口期都返回 false，host 直接走原始 dispatchDraw 不裁剪，
     * 避免「mask 跳到新 PTS 而视频还停在旧位置」的可见错位。
     */
    fun shouldRenderMask(): Boolean {
        if (!enabled || !maskReady || currentCid <= 0L) return false
        if (shouldSkipForSeek()) return false
        return true
    }

    fun setPlaying(playing: Boolean) {
        if (isPlaying == playing) return
        isPlaying = playing
    }

    fun setPlaybackSpeed(speed: Float) {
        if (speed.isFinite() && speed > 0f) {
            playbackSpeed = speed
        }
    }

    fun onVideoFrameAnchor(presentationTimeUs: Long, releaseTimeNs: Long) {
        // VideoFrameMetadataListener 协议：
        //   releaseTimeNs == 0      → "立刻 release"（特殊指令值）
        //   releaseTimeNs == MAX    → "暂不 release"（特殊指令值）
        // 都不能当 wall clock 用——用它做差会算出几十亿 ns 的 deltaNs，让 mask PTS 飞到几十秒后。
        // 直接退化成 nanoTime()，让 deltaNs ≈ 0，mask 用 anchor.pts 渲染（基本对齐）。
        val safeReleaseNs = if (releaseTimeNs > 0L && releaseTimeNs < Long.MAX_VALUE) {
            releaseTimeNs
        } else {
            System.nanoTime()
        }
        anchorPresentationTimeUs = presentationTimeUs
        anchorReleaseTimeNs = safeReleaseNs
        hasVideoAnchor = true

        // 顺手测 anchor 推送间隔：间隔异常大（如 > 80ms）意味着 video 解码卡顿，
        // mask 外推距离过远，必然错位——日志输出后用户能立刻判断是 video 端的问题。
        recordAnchorIntervalAndMaybeWarn(safeReleaseNs)

        // anchor 是视频解码侧 60Hz 推送的高频回调——天然是「播放推进」的真实信号。
        // 利用它驱动段进度预解析，避免「pushMaskUpdate 只在 seek 触发」导致播放跨段后
        // timeline 永远查不到 cachedFrames 的死局。
        maybePreloadAroundCurrentPts(presentationTimeUs / 1000L)
    }

    @Volatile
    private var lastAnchorReleaseNs: Long = 0L
    @Volatile
    private var anchorIntervalMsEma: Long = 0L

    private fun recordAnchorIntervalAndMaybeWarn(releaseNs: Long) {
        val prev = lastAnchorReleaseNs
        lastAnchorReleaseNs = releaseNs
        if (prev <= 0L) return
        val intervalMs = (releaseNs - prev) / 1_000_000L
        // 5~150ms 之间是正常视频帧间隔（6.7~200fps）；外面的极可能是 seek 跳变 / 渲染抖动。
        if (intervalMs !in 5L..150L) return
        anchorIntervalMsEma = if (anchorIntervalMsEma == 0L) intervalMs
        else (anchorIntervalMsEma * 7 + intervalMs) / 8
    }

    /**
     * 由 [onVideoFrameAnchor] 在 playback thread 调用。
     * 跨段或下一段未缓存时，去重 launch 一次后台预解析（current ± 2 段）。
     */
    private fun maybePreloadAroundCurrentPts(ptsMs: Long) {
        val timeline = currentTimeline ?: return
        val segIdx = timeline.segmentIndexAt(ptsMs)
        if (segIdx == preloadingSegIndex) return
        // 当前段及下一段都已缓存 → 无需 launch（避免每秒 60 次无效协程提交）。
        if (timeline.isSegmentCached(segIdx) &&
            timeline.isSegmentCached(segIdx + 1)
        ) {
            lastPreloadedSegIndex = segIdx
            return
        }
        preloadAhead(segIdx)
        lastPreloadedSegIndex = segIdx
    }

    fun setPlaybackReady(ready: Boolean) {
        if (playbackReady == ready) return
        playbackReady = ready
        if (ready) {
            if (awaitingSeekReady && seekReadyAt == 0L) {
                seekReadyAt = SystemClock.elapsedRealtime() + SEEK_READY_STABILIZE_MS
            }
        } else {
            seekReadyAt = 0L
        }
    }

    fun onSeek() {
        awaitingSeekReady = true
        seekReadyAt = if (playbackReady) {
            SystemClock.elapsedRealtime() + SEEK_READY_STABILIZE_MS
        } else {
            0L
        }
        seekHardDeadlineMs = SystemClock.elapsedRealtime() + SEEK_RECOVER_HARD_TIMEOUT_MS
        hasVideoAnchor = false
        lastPreloadedSegIndex = -1
        clearMask()
    }

    fun onPositionChanged(positionMs: Long) {
        if (!enabled || !maskReady) return
        if (shouldSkipForSeek()) {
            clearMask()
            return
        }
        invalidateMaskHost()
    }

    fun setRepository(repository: DmMaskRepository) {
        this.repository = repository
    }

    fun release() {
        currentCid = 0L
        maskReady = false
        currentTimeline = null
        lastPreloadedSegIndex = -1
        clearMask()
    }

    fun dispose() {
        release()
    }

    // ---- 内部实现 ----

    private fun checkAndPreloadNext() {
        val timeline = currentTimeline ?: return
        val pts = currentVideoPtsMs()
        val segIdx = timeline.segmentIndexAt(pts)
        if (segIdx > lastPreloadedSegIndex || !timeline.isSegmentCached(segIdx)) {
            preloadAhead(segIdx)
            lastPreloadedSegIndex = segIdx
        }
    }

    private fun preloadAhead(currentSegIdx: Int) {
        val cid = currentCid
        val timeline = currentTimeline ?: return
        if (preloadingSegIndex == currentSegIdx) return
        preloadingSegIndex = currentSegIdx
        val totalSegs = timeline.totalSegments()
        // 预解析当前段 ± 2
        val range = (currentSegIdx - 1).coerceAtLeast(0)..(currentSegIdx + 2).coerceAtMost(totalSegs - 1)
        preloadScope.launch {
            try {
                for (idx in range) {
                    repository.preloadSegmentFrames(cid, idx)
                }
            } finally {
                // 释放去重锁，让下次跨段能再 launch；用 == 是为了避免覆盖更新的 segIdx。
                if (preloadingSegIndex == currentSegIdx) preloadingSegIndex = -1
            }
        }
    }

    private fun shouldSkipForSeek(): Boolean {
        if (!awaitingSeekReady) return false
        val now = SystemClock.elapsedRealtime()
        if (now >= seekHardDeadlineMs) {
            awaitingSeekReady = false
            return false
        }
        if (seekReadyAt > 0L && now >= seekReadyAt) {
            awaitingSeekReady = false
            return false
        }
        return true
    }

    private fun invalidateMaskHost() {
        maskHostProvider()?.invalidate()
    }

    private fun clearMask() {
        maskHostProvider()?.invalidate()
    }
}
