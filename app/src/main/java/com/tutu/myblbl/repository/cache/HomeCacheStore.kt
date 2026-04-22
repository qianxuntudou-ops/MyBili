package com.tutu.myblbl.repository.cache

import com.google.gson.reflect.TypeToken
import com.tutu.myblbl.core.common.log.HomeVideoCardDebugLogger
import com.tutu.myblbl.model.lane.HomeLaneSection
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.core.common.cache.FileCacheManager

object HomeCacheStore {

    suspend fun readVideos(cacheKey: String): List<VideoModel> {
        val type = object : TypeToken<List<VideoModel>>() {}.type
        val cachedVideos = FileCacheManager.getAsync<List<VideoModel>>(cacheKey, type).orEmpty()
        HomeVideoCardDebugLogger.logRejectedCards(
            source = "home_cache($cacheKey)",
            items = cachedVideos
        )
        return cachedVideos.filter { it.isSupportedHomeVideoCard }
    }

    suspend fun writeVideos(cacheKey: String, videos: List<VideoModel>) {
        FileCacheManager.putAsync(cacheKey, videos)
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
