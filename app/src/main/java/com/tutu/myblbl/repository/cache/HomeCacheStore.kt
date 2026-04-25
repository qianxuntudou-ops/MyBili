package com.tutu.myblbl.repository.cache

import com.google.gson.reflect.TypeToken
import com.tutu.myblbl.core.common.log.HomeVideoCardDebugLogger
import com.tutu.myblbl.model.lane.HomeLaneSection
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.core.common.cache.FileCacheManager

object HomeCacheStore {

    private const val VIDEO_CACHE_SCHEMA_VERSION = 1

    data class CachedVideos(
        val items: List<VideoModel>,
        val savedAtMs: Long = 0L,
        val schemaVersion: Int = 0
    )

    private data class VideoCacheEnvelope(
        val schemaVersion: Int = VIDEO_CACHE_SCHEMA_VERSION,
        val savedAtMs: Long = System.currentTimeMillis(),
        val items: List<VideoModel> = emptyList()
    )

    suspend fun readVideos(cacheKey: String): List<VideoModel> {
        return readCachedVideos(cacheKey).items
    }

    suspend fun readCachedVideos(cacheKey: String): CachedVideos {
        val envelopeType = object : TypeToken<VideoCacheEnvelope>() {}.type
        val envelope = FileCacheManager.getAsync<VideoCacheEnvelope>(cacheKey, envelopeType)
        if (envelope != null && envelope.schemaVersion == VIDEO_CACHE_SCHEMA_VERSION) {
            return CachedVideos(
                items = filterVideoCache(cacheKey, envelope.items),
                savedAtMs = envelope.savedAtMs,
                schemaVersion = envelope.schemaVersion
            )
        }

        val type = object : TypeToken<List<VideoModel>>() {}.type
        val cachedVideos = FileCacheManager.getAsync<List<VideoModel>>(cacheKey, type).orEmpty()
        return CachedVideos(
            items = filterVideoCache(cacheKey, cachedVideos),
            savedAtMs = 0L,
            schemaVersion = 0
        )
    }

    private fun filterVideoCache(cacheKey: String, cachedVideos: List<VideoModel>): List<VideoModel> {
        HomeVideoCardDebugLogger.logRejectedCards(
            source = "home_cache($cacheKey)",
            items = cachedVideos
        )
        return cachedVideos.filter { it.isSupportedHomeVideoCard }
    }

    suspend fun writeVideos(cacheKey: String, videos: List<VideoModel>) {
        FileCacheManager.putAsync(
            cacheKey,
            VideoCacheEnvelope(
                savedAtMs = System.currentTimeMillis(),
                items = videos
            )
        )
    }

    suspend fun readSections(cacheKey: String): List<HomeLaneSection> {
        val type = object : TypeToken<List<HomeLaneSection>>() {}.type
        return FileCacheManager.getAsync<List<HomeLaneSection>>(cacheKey, type)
            .orEmpty()
            .filter { it.items.isNotEmpty() }
    }

    suspend fun writeSections(cacheKey: String, sections: List<HomeLaneSection>) {
        FileCacheManager.putAsync(cacheKey, sections)
    }
}
