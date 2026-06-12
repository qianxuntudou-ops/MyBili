package com.tutu.myblbl.feature.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

@OptIn(UnstableApi::class)
class VideoPlayerDashMediaSourceFactory(
    private val dataSourceFactory: DataSource.Factory,
    private val urlNormalizer: (String) -> String
) {

    companion object {
    }

    private val loadErrorPolicy = object : LoadErrorHandlingPolicy {
        override fun getFallbackSelectionFor(
            options: LoadErrorHandlingPolicy.FallbackOptions,
            errorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
        ): LoadErrorHandlingPolicy.FallbackSelection? = null

        override fun getRetryDelayMsFor(
            errorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
        ): Long = 500L

        override fun getMinimumLoadableRetryCount(dataType: Int): Int = 5
    }

    fun createMediaSource(route: DashRoute): MediaSource {
        val videoSource = createProgressiveSource(
            urls = route.videoUrls,
            mimeType = route.videoRepresentation.mimeType
        )

        val audioSource = route.audioRepresentation?.let {
            if (route.audioUrls.isNotEmpty()) {
                createProgressiveSource(
                    urls = route.audioUrls,
                    mimeType = it.mimeType
                )
            } else null
        }

        val mediaSource = if (audioSource != null) {
            MergingMediaSource(true, videoSource, audioSource)
        } else {
            videoSource
        }
        return mediaSource
    }

    private fun createProgressiveSource(urls: List<String>, mimeType: String): MediaSource {
        val primaryUrl = urls.firstOrNull()
            ?: error("No media url candidates available")
        val sourceFactory = createCandidateAwareFactory(urls)
        val mediaItem = MediaItem.Builder()
            .setUri(primaryUrl)
            .setMimeType(mimeType.takeIf { it.isNotBlank() })
            .build()
        return ProgressiveMediaSource.Factory(sourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorPolicy)
            .createMediaSource(mediaItem)
    }

    private fun createCandidateAwareFactory(urls: List<String>): DataSource.Factory {
        val candidates = urls
            .map(urlNormalizer)
            .filter { it.isNotBlank() }
            .distinct()
        if (candidates.size <= 1) {
            return dataSourceFactory
        }
        return VideoPlayerCdnFailoverDataSourceFactory(
            upstreamFactory = dataSourceFactory,
            state = VideoPlayerCdnFailoverState(
                candidates = candidates.map(Uri::parse)
            )
        )
    }
}
