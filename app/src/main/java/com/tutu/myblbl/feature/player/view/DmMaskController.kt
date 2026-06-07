package com.tutu.myblbl.feature.player.view

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import com.tutu.myblbl.core.common.log.AppLog
import com.tutu.myblbl.model.dm.DmMaskRepository
import com.tutu.myblbl.model.dm.DmMaskTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 弹幕防挡蒙版控制器。
 *
 * 按 Bilibili 参考链路完全重构：
 *  - 播放器 clock（onPlayerClockChanged / player.currentPosition）是唯一时间源
 *  - 清晰的状态机：IDLE → READY → ACTIVE → SEEKING → ACTIVE
 *  - 预加载跟随 player clock，不跟 video frame PTS
 *  - Choreographer 只驱动 ACTIVE 状态下的 mask 重绘，不因 UI 卡顿自动关闭 mask
 */
class DmMaskController(
    private val maskHostProvider: () -> DanmakuMaskHostLayout?,
    private var repository: DmMaskRepository,
) {
    companion object {
        private const val TAG = "DmMaskController"

        /** seek 后等待 playbackReady 的硬超时。 */
        private const val SEEK_HARD_TIMEOUT_MS = 1500L

        /** 诊断日志节流间隔。 */
        private const val DIAG_LOG_INTERVAL_MS = 5000L
        private const val PRELOAD_DIAG_INTERVAL_MS = 1000L

    }

    // ---- 状态 ----

    private enum class State { IDLE, READY, ACTIVE, SEEKING }

    private var state = State.IDLE
    private var currentCid: Long = 0L
    private var currentTimeline: DmMaskTimeline? = null
    private var isPlaying: Boolean = false
    private var playbackReady: Boolean = false
    private var enabled: Boolean = false

    // ---- 时钟 ----

    /**
     * 播放器 position 提供者。由外部注入 `player?.currentPosition`。
     * 这是唯一的时间源（参考文档结论 1）。
     */
    var playerPositionProvider: (() -> Long)? = null

    @Volatile
    private var playbackSpeed: Float = 1.0f

    /**
     * 参考 Chronos 的 playback clock：onPlayerClockChanged(rate, position) 只校准基准点，
     * 后续查询用 elapsedRealtime 按 rate 连续推进，避免 Media3 currentPosition 刷新粒度造成
     * mask 卡顿或追不上视频帧。
     */
    @Volatile
    private var clockBasePositionMs: Long = 0L

    @Volatile
    private var clockBaseRealtimeMs: Long = 0L

    // ---- Seek 状态 ----

    private var seekHardDeadlineMs: Long = 0L

    // ---- 预加载 ----

    private var lastPreloadedSegIndex: Int = -1

    @Volatile
    private var preloadingSegIndex: Int = -1

    private val frameInvalidator = FrameInvalidator()

    // ---- 诊断 ----

    @Volatile
    private var diagEnabled: Boolean = false

    @Volatile
    private var lastDiagLogMs: Long = 0L
    private var lastPreloadDiagMs: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ========== 公开 API ==========

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        when {
            !enabled -> {
                state = State.IDLE
                maskHostProvider()?.clearCachedMask()
                frameInvalidator.stop()
            }
            currentTimeline != null -> {
                state = if (isPlaying) State.ACTIVE else State.READY
                frameInvalidator.start()
                maybePreload(currentVideoPtsMs())
                invalidateMaskHost()
            }
        }
    }

    suspend fun loadMask(maskUrl: String, cid: Long, fps: Int): Boolean {
        currentCid = cid
        state = State.IDLE
        lastPreloadedSegIndex = -1
        maskHostProvider()?.clearCachedMask()

        val data = repository.downloadAndParse(maskUrl, cid, fps)
        if (data == null) {
            AppLog.e(TAG, "Mask load failed for cid=$cid")
            return false
        }

        currentTimeline = repository.getTimeline(cid)
        maskHostProvider()?.let { it.timeline = currentTimeline }

        state = if (!enabled) {
            State.IDLE
        } else if (isPlaying) {
            State.ACTIVE
        } else {
            State.READY
        }

        if (enabled) {
            frameInvalidator.start()
            invalidateMaskHost()
            // 触发初始预加载
            maybePreload(currentVideoPtsMs())
        }
        return true
    }

    /**
     * 唯一时钟入口。由播放器的 onPlayerClockChanged / syncDanmakuPosition 调用。
     * 对齐参考：onPlayerClockChanged(rate, position)。
     */
    fun onPlayerClockChanged(rate: Float, positionMs: Long) {
        if (rate.isFinite() && rate > 0f) {
            playbackSpeed = rate
        }
        clockBasePositionMs = positionMs.coerceAtLeast(0L)
        clockBaseRealtimeMs = SystemClock.elapsedRealtime()
        if (state == State.ACTIVE) {
            maybePreload(currentVideoPtsMs())
            invalidateMaskHost()
        }
    }

    /** 进度更新时调用（由 syncDanmakuPosition 调用）。 */
    fun pushMaskUpdate() {
        if (state != State.ACTIVE) return
        checkAndPreloadNext()
        invalidateMaskHost()
    }

    /** Seek 操作。进入 SEEKING 状态，保留旧遮罩冻结（不清除），等新 segment 加载后自动替换。 */
    fun onSeek() {
        state = State.SEEKING
        seekHardDeadlineMs = SystemClock.elapsedRealtime() + SEEK_HARD_TIMEOUT_MS
        lastPreloadedSegIndex = -1
        // 不调用 clearCachedMask()：旧遮罩在新 segment 加载完成前继续显示，
        // 避免 seek 后 100~300ms 的无遮罩窗口。
    }

    fun setPlaying(playing: Boolean) {
        if (!playing && isPlaying) {
            clockBasePositionMs = currentVideoPtsMs()
            clockBaseRealtimeMs = SystemClock.elapsedRealtime()
        }
        isPlaying = playing
        if (playing && state == State.READY && enabled) {
            state = State.ACTIVE
            frameInvalidator.start()
            maybePreload(currentVideoPtsMs())
            invalidateMaskHost()
        } else if (playing && state == State.ACTIVE && enabled) {
            maybePreload(currentVideoPtsMs())
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (speed.isFinite() && speed > 0f) {
            playbackSpeed = speed
            lastPreloadedSegIndex = -1  // 速度变了，重新触发预加载
        }
    }

    fun setPlaybackReady(ready: Boolean) {
        playbackReady = ready
        if (ready && state == State.SEEKING) {
            transitionFromSeeking()
        }
    }

    fun onPositionChanged(positionMs: Long) {
        if (state == State.SEEKING && playbackReady) {
            transitionFromSeeking()
        }
        if (state == State.ACTIVE) {
            invalidateMaskHost()
        }
    }

    /**
     * 返回 mask 当前应该查询 timeline 的 PTS（ms）。
     * 唯一时间源：参考 Chronos 的校准点 + elapsedRealtime 外推。
     */
    fun currentVideoPtsMs(): Long {
        val base = if (clockBaseRealtimeMs > 0L) {
            clockBasePositionMs
        } else {
            (playerPositionProvider?.invoke() ?: 0L).coerceAtLeast(0L)
        }
        if (!isPlaying || clockBaseRealtimeMs <= 0L) {
            return base.coerceAtLeast(0L)
        }
        val elapsedMs = (SystemClock.elapsedRealtime() - clockBaseRealtimeMs).coerceAtLeast(0L)
        return (base + (elapsedMs * playbackSpeed).toLong()).coerceAtLeast(0L)
    }

    /**
     * HostLayout 调用：是否应该渲染遮罩。
     * IDLE → false。READY / ACTIVE / SEEKING → true（SEEKING 时用旧遮罩冻结）。
     */
    fun shouldRenderMask(): Boolean {
        return enabled && state != State.IDLE
    }

    /** HostLayout 调用：当前是否处于 SEEKING 冻结状态。 */
    fun isSeeking(): Boolean {
        return state == State.SEEKING
    }

    fun setRepository(repository: DmMaskRepository) {
        this.repository = repository
    }

    /** 后台回前台时调用：ACTIVE 状态下恢复 mask 重绘。 */
    fun onResume() {
        if (state == State.ACTIVE) {
            frameInvalidator.start()
            invalidateMaskHost()
        }
    }

    fun release() {
        state = State.IDLE
        currentCid = 0L
        currentTimeline = null
        lastPreloadedSegIndex = -1
        frameInvalidator.stop()
        maskHostProvider()?.let { host ->
            host.timeline = null
            host.clearCachedMask()
        }
    }

    fun dispose() {
        release()
    }

    // ---- 诊断 API（保持兼容） ----

    fun reportFrameQuery(queryPtsMs: Long, framePtsMs: Long) {
        if (!diagEnabled) return
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastDiagLogMs < DIAG_LOG_INTERVAL_MS) return
        lastDiagLogMs = nowMs
        val playerPos = playerPositionProvider?.invoke() ?: -1L
        AppLog.d(
            TAG,
            "pts diag: query=$queryPtsMs frame.pts=$framePtsMs frame-query=${framePtsMs - queryPtsMs}ms " +
                "playerPos=$playerPos speed=$playbackSpeed state=$state"
        )
    }

    fun setDiagEnabled(enabled: Boolean) {
        diagEnabled = enabled
    }

    // 以下为兼容空实现（参考方案不用这些，但 MyPlayerView 有接线）
    fun reportFramePipelineDelay(@Suppress("UNUSED_PARAMETER") totalDurationNs: Long) {}
    fun reportVsyncPeriod(@Suppress("UNUSED_PARAMETER") periodNs: Long) {}
    fun onVideoFrameAnchor(
        @Suppress("UNUSED_PARAMETER") presentationTimeUs: Long,
        @Suppress("UNUSED_PARAMETER") releaseTimeNs: Long,
    ) {}

    // ========== 内部实现 ==========

    private fun transitionFromSeeking() {
        state = if (enabled && isPlaying) State.ACTIVE else State.READY
        if (state == State.ACTIVE) {
            maybePreload(currentVideoPtsMs())
            invalidateMaskHost()
        }
    }

    private fun checkAndPreloadNext() {
        val timeline = currentTimeline ?: return
        val pts = currentVideoPtsMs()
        val segIdx = timeline.segmentIndexAt(pts)
        if (segIdx > lastPreloadedSegIndex || !timeline.isSegmentCached(segIdx)) {
            preloadAhead(segIdx)
            lastPreloadedSegIndex = segIdx
        }
    }

    /**
     * 预加载：当前段 ± 2。由 player clock 驱动。
     */
    private fun maybePreload(ptsMs: Long) {
        val timeline = currentTimeline ?: return
        val segIdx = timeline.segmentIndexAt(ptsMs)
        if (segIdx == preloadingSegIndex) return
        if (timeline.isSegmentCached(segIdx) && timeline.isSegmentCached(segIdx + 1)) {
            lastPreloadedSegIndex = segIdx
            return
        }
        preloadAhead(segIdx)
        lastPreloadedSegIndex = segIdx
    }

    private fun preloadAhead(currentSegIdx: Int) {
        val cid = currentCid
        val timeline = currentTimeline ?: return
        if (preloadingSegIndex == currentSegIdx) return
        preloadingSegIndex = currentSegIdx
        val totalSegs = timeline.totalSegments()
        val orderedSegments = listOf(
            currentSegIdx,
            currentSegIdx + 1,
            currentSegIdx - 1,
            currentSegIdx + 2
        ).filter { it in 0 until totalSegs }.distinct()
        maybeLogPreload(currentSegIdx, orderedSegments)
        preloadScope.launch {
            try {
                for (idx in orderedSegments) {
                    repository.preloadSegmentFrames(cid, idx)
                }
            } finally {
                if (preloadingSegIndex == currentSegIdx) preloadingSegIndex = -1
            }
        }
    }

    private fun maybeLogPreload(currentSegIdx: Int, orderedSegments: List<Int>) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPreloadDiagMs < PRELOAD_DIAG_INTERVAL_MS) return
        lastPreloadDiagMs = now
        AppLog.d(
            TAG,
            "preload mask segments: state=$state cid=$currentCid current=$currentSegIdx " +
                "order=$orderedSegments pts=${currentVideoPtsMs()} enabled=$enabled playing=$isPlaying"
        )
    }

    private fun invalidateMaskHost() {
        maskHostProvider()?.postInvalidateOnAnimation()
    }

    // ========== FrameInvalidator ==========

    /**
     * 基于 Choreographer 驱动弹幕 mask 每个 vsync 重绘。
     * 参考不会因为 UI 卡顿自动关闭 mask；卡顿只会推迟下一帧提交。
     */
    private inner class FrameInvalidator : Choreographer.FrameCallback {
        @Volatile
        private var running: Boolean = false
        private var choreographer: Choreographer? = null

        fun start() {
            mainHandler.post {
                if (running) return@post
                running = true
                val ch = choreographer ?: Choreographer.getInstance().also { choreographer = it }
                ch.postFrameCallback(this)
            }
        }

        fun stop() {
            mainHandler.post {
                if (!running) return@post
                running = false
                choreographer?.removeFrameCallback(this)
            }
        }

        override fun doFrame(frameTimeNs: Long) {
            if (!running) return
            // ACTIVE 状态下每 vsync invalidate mask host，保证遮罩实时更新
            if (state == State.ACTIVE) {
                invalidateMaskHost()
            }
            choreographer?.postFrameCallback(this)
        }
    }
}
