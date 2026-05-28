package com.tutu.myblbl.model.dm

import android.util.LruCache
import com.tutu.myblbl.core.common.log.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class DmMaskRepository {

    companion object {
        private const val TAG = "DmMaskRepository"
        private const val MAX_CACHE_SIZE = 3
    }

    private val cache = LruCache<Long, DmMaskData>(MAX_CACHE_SIZE)
    private val timelineCache = LruCache<Long, DmMaskTimeline>(MAX_CACHE_SIZE)
    private val segmentParseLocks = ConcurrentHashMap<String, Any>()

    suspend fun downloadAndParse(maskUrl: String, cid: Long, fps: Int): DmMaskData? {
        cache.get(cid)?.let {
            AppLog.d(TAG, "Webmask cache hit: cid=$cid")
            return it
        }
        return withContext(Dispatchers.IO) {
            try {
                val url = if (maskUrl.startsWith("//")) "https:$maskUrl" else maskUrl
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                connection.requestMethod = "GET"

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    AppLog.e(TAG, "Download webmask failed: ${connection.responseCode}")
                    return@withContext null
                }

                val data = connection.inputStream.readBytes()
                connection.disconnect()

                val maskData = WebmaskParser.parse(data, fps)
                if (maskData != null) {
                    // 预解析所有 segment 并构建时间线
                    val timeline = DmMaskTimeline.build(maskData)
                    if (timeline != null) {
                        timelineCache.put(cid, timeline)
                    }
                    cache.put(cid, maskData)
                    AppLog.d(TAG, "Webmask parsed: cid=$cid, segments=${maskData.rawSegments.size}, " +
                        "fps=$fps, timelineSegs=${timeline?.totalSegments() ?: 0}")
                }
                maskData
            } catch (e: Exception) {
                AppLog.e(TAG, "Download webmask error: ${e.message}")
                null
            }
        }
    }

    /** 通过时间线查询指定 PTS 最近的帧（O(log N)），供新的 clipOutPath 渲染路径使用。 */
    fun queryTimelineFrame(cid: Long, ptsMs: Long): MaskFrame? {
        return timelineCache.get(cid)?.queryAt(ptsMs)
    }

    fun getTimeline(cid: Long): DmMaskTimeline? = timelineCache.get(cid)

    data class FrameResult(
        val frame: MaskFrame,
        val segIndex: Int,
        val frameIndex: Int,
        val segStartTimeMs: Long,
        val segDurationMs: Long,
        val totalFrames: Int,
        val totalSegments: Int
    )

    fun queryFrameWithIndex(cid: Long, positionMs: Long): FrameResult? {
        val maskData = cache.get(cid) ?: return null
        val segments = maskData.rawSegments
        if (segments.isEmpty()) return null

        val segIndex = segments.binarySearchBy(positionMs) { it.timeMs }
            .let { if (it < 0) -(it + 1) - 1 else it }
            .coerceIn(0, segments.lastIndex)
        val segment = segments[segIndex]

        var frames = segment.cachedFrames
        if (frames == null) {
            val segDurationMs = if (segIndex + 1 < segments.size) {
                (segments[segIndex + 1].timeMs - segment.timeMs).coerceAtLeast(1)
            } else {
                (300L * 1000L / maskData.fps.coerceAtLeast(1)).coerceAtLeast(1)
            }
            frames = WebmaskParser.parseSegmentFrames(segment, maskData.fps, segDurationMs) ?: emptyList()
            segment.cachedFrames = frames
            AppLog.d(TAG, "Segment parsed: seg=$segIndex, timeMs=${segment.timeMs}, frames=${frames.size}")
        }
        if (frames.isEmpty()) return null

        val offsetMs = positionMs - segment.timeMs
        val segDurationMs = if (segIndex + 1 < segments.size) {
            (segments[segIndex + 1].timeMs - segment.timeMs).coerceAtLeast(1)
        } else {
            (frames.size.toLong() * 1000L / maskData.fps.coerceAtLeast(1)).coerceAtLeast(1)
        }
        val frameIndex = ((offsetMs * frames.size + segDurationMs / 2) / segDurationMs).toInt()
            .coerceIn(0, frames.size - 1)

        val frame = frames.getOrNull(frameIndex) ?: return null
        return FrameResult(
            frame = frame, segIndex = segIndex, frameIndex = frameIndex,
            segStartTimeMs = segment.timeMs, segDurationMs = segDurationMs,
            totalFrames = frames.size, totalSegments = segments.size
        )
    }

    fun preloadSegmentFrames(cid: Long, segIndex: Int) {
        val maskData = cache.get(cid) ?: return
        val segments = maskData.rawSegments
        if (segIndex < 0 || segIndex >= segments.size) return
        val segment = segments[segIndex]
        if (segment.cachedFrames != null) return
        val lockKey = "$cid:$segIndex"
        val lock = segmentParseLocks.getOrPut(lockKey) { Any() }
        try {
            synchronized(lock) {
                if (segment.cachedFrames != null) return
                val segDurationMs = if (segIndex + 1 < segments.size) {
                    (segments[segIndex + 1].timeMs - segment.timeMs).coerceAtLeast(1)
                } else {
                    (300L * 1000L / maskData.fps.coerceAtLeast(1)).coerceAtLeast(1)
                }
                val frames = WebmaskParser.parseSegmentFrames(segment, maskData.fps, segDurationMs) ?: emptyList()
                segment.cachedFrames = frames
                AppLog.d(TAG, "Segment preloaded: seg=$segIndex, frames=${frames.size}")
            }
        } finally {
            segmentParseLocks.remove(lockKey, lock)
        }
    }

    fun clear(cid: Long) {
        cache.remove(cid)
        timelineCache.remove(cid)
        segmentParseLocks.keys.removeAll { it.startsWith("$cid:") }
    }

    fun clearAll() {
        cache.evictAll()
        timelineCache.evictAll()
        segmentParseLocks.clear()
    }
}
