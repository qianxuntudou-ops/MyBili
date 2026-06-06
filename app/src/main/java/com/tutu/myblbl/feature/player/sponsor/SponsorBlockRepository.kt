package com.tutu.myblbl.feature.player.sponsor

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tutu.myblbl.core.common.log.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object SponsorBlockRepository {

    private const val BASE_URL = "https://bsbsb.top/api"
    private const val TAG = "SponsorBlock"
    private const val CACHE_TTL_MS = 30 * 60 * 1000L
    private const val CONNECTION_ERROR = "空降助手连接失败，请检查网络"

    private val gson = Gson()

    private data class CacheEntry(
        val segments: List<SponsorSegment>,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    data class HashVideoResponse(
        val videoID: String = "",
        val segments: List<SponsorSegment> = emptyList()
    )

    suspend fun testConnection(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/status")
                .header("User-Agent", "MyBLBL/1.0")
                .header("origin", "com.tutu.myblbl")
                .header("x-ext-version", "0.13.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 200) null else "空降助手服务异常 (${response.code})"
            }
        } catch (e: IOException) {
            AppLog.e(TAG, "连通性测试失败: ${e.message}")
            CONNECTION_ERROR
        } catch (e: Exception) {
            AppLog.e(TAG, "连通性测试异常: ${e.message}", e)
            "空降助手测试失败"
        }
    }

    suspend fun testFetch(): String = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/skipSegments/a1b2?category=sponsor"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MyBLBL/1.0")
                .header("origin", "com.tutu.myblbl")
                .header("x-ext-version", "0.13.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code != 200) return@withContext "测试失败：API 返回 ${response.code}"

                val body = response.body?.string() ?: return@withContext "测试失败：响应为空"
                val type = object : TypeToken<List<HashVideoResponse>>() {}.type
                val videos = gson.fromJson<List<HashVideoResponse>>(body, type)
                val totalSegments = videos.sumOf { it.segments.size }
                return@withContext if (totalSegments > 0) {
                    "测试通过：解析正常，获取到 $totalSegments 个片段"
                } else {
                    "测试通过：连接和解析正常（当前无测试数据）"
                }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "片段拉取测试失败: ${e.message}", e)
            "测试失败：${e.message}"
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class SegmentResult(
        val segments: List<SponsorSegment> = emptyList(),
        val error: String? = null
    )

    suspend fun getSegments(
        bvid: String,
        cid: Long = 0L,
        categories: List<String> = SponsorSegment.ALL_CATEGORIES
    ): SegmentResult = withContext(Dispatchers.IO) {
        val cacheKey = "$bvid:$cid"
        cache[cacheKey]?.let { entry ->
            if (System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
                return@withContext SegmentResult(entry.segments)
            }
            cache.remove(cacheKey)
        }

        try {
            val hashPrefix = sha256Prefix(bvid)
            val params = buildList {
                categories.forEach { add("category=$it") }
            }
            val url = "$BASE_URL/skipSegments/$hashPrefix?${params.joinToString("&")}"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MyBLBL/1.0")
                .header("origin", "com.tutu.myblbl")
                .header("x-ext-version", "0.13.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: return@withContext SegmentResult()
                        val type = object : TypeToken<List<HashVideoResponse>>() {}.type
                        val videos = gson.fromJson<List<HashVideoResponse>>(body, type)
                        val allSegments = selectSegments(videos, bvid, cid)
                        val segments = normalizeSegments(allSegments)
                        cache[cacheKey] = CacheEntry(segments)
                        SegmentResult(segments)
                    }
                    404 -> SegmentResult()
                    else -> {
                        AppLog.w(TAG, "$bvid: API error ${response.code}")
                        SegmentResult(error = "空降助手服务异常 (${response.code})")
                    }
                }
            }
        } catch (e: IOException) {
            AppLog.w(TAG, "$bvid: ${e.message}")
            SegmentResult(error = CONNECTION_ERROR)
        } catch (e: Exception) {
            AppLog.w(TAG, "$bvid: ${e.message}", e)
            SegmentResult(error = "空降助手加载失败")
        }
    }

    private fun sha256Prefix(input: String, prefixLength: Int = 4): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(prefixLength)
    }

    internal fun selectSegments(
        videos: List<HashVideoResponse>,
        bvid: String,
        cid: Long
    ): List<SponsorSegment> {
        return videos
            .find { it.videoID == bvid }
            ?.segments
            ?.filter { segment -> cid <= 0L || segment.cid == cid.toString() }
            ?: emptyList()
    }

    private fun normalizeSegments(
        segments: List<SponsorSegment>
    ): List<SponsorSegment> {
        return segments
            .filter { it.isSkipType && it.endTimeMs > it.startTimeMs }
            .groupBy { it.UUID.ifBlank { "${it.category}:${it.startTimeMs}:${it.endTimeMs}" } }
            .mapNotNull { (_, duplicates) ->
                duplicates.maxWithOrNull(
                    compareBy<SponsorSegment> { it.locked }
                        .thenBy { it.votes }
                )
            }
            .sortedBy { it.startTimeMs }
    }
}
