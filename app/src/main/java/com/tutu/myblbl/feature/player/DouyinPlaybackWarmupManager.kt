package com.tutu.myblbl.feature.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import com.tutu.myblbl.core.common.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
internal class DouyinPlaybackWarmupManager(
    private val dataSourceFactory: DataSource.Factory,
    private val urlNormalizer: (String) -> String
) {
    companion object {
        private const val TAG = "DouyinWarmup"
        private const val VIDEO_PREFETCH_BYTES = 6L * 1024L * 1024L
        private const val AUDIO_PREFETCH_BYTES = 2L * 1024L * 1024L
        private const val PROGRESSIVE_PREFETCH_BYTES = 8L * 1024L * 1024L
        private const val BUFFER_SIZE = 32 * 1024
    }

    suspend fun warmup(playInfo: com.tutu.myblbl.model.player.PlayInfoModel, selection: VideoPlayerStreamResolver.SelectionSnapshot) {
        val targets = buildTargets(playInfo, selection)
        if (targets.isEmpty()) {
            return
        }

        coroutineScope {
            targets.forEach { target ->
                launch {
                    prefetch(target)
                }
            }
        }
    }

    private fun buildTargets(
        playInfo: com.tutu.myblbl.model.player.PlayInfoModel,
        selection: VideoPlayerStreamResolver.SelectionSnapshot
    ): List<PrefetchTarget> {
        val dash = playInfo.dash
        if (dash != null && !dash.video.isNullOrEmpty()) {
            val selectedVideo = dash.video.orEmpty()
                .filter { selection.selectedQualityId == null || it.id == selection.selectedQualityId }
                .filter { selection.selectedCodec == null || it.codecId == selection.selectedCodec.id }
                .maxByOrNull { it.bandwidth }
                ?: dash.video.orEmpty()
                    .filter { selection.selectedQualityId == null || it.id == selection.selectedQualityId }
                    .maxByOrNull { it.bandwidth }
                ?: dash.video.orEmpty().maxByOrNull { it.bandwidth }

            val selectedAudio = dash.audio.orEmpty()
                .firstOrNull { selection.selectedAudioId != null && it.id == selection.selectedAudioId }
                ?: dash.audio.orEmpty().maxByOrNull { it.bandwidth }

            return buildList {
                selectedVideo?.realBaseUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add(PrefetchTarget(kind = "video", url = it, bytes = VIDEO_PREFETCH_BYTES)) }
                selectedAudio?.realBaseUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add(PrefetchTarget(kind = "audio", url = it, bytes = AUDIO_PREFETCH_BYTES)) }
            }
        }

        val durl = playInfo.durl?.firstOrNull()?.url?.takeIf { it.isNotBlank() }
        return durl?.let {
            listOf(PrefetchTarget(kind = "progressive", url = it, bytes = PROGRESSIVE_PREFETCH_BYTES))
        }.orEmpty()
    }

    private fun prefetch(target: PrefetchTarget) {
        val normalizedUrl = urlNormalizer(target.url)
        if (normalizedUrl.isBlank()) return

        val startedAt = System.currentTimeMillis()
        val dataSource = dataSourceFactory.createDataSource()
        val dataSpec = DataSpec.Builder()
            .setUri(Uri.parse(normalizedUrl))
            .setPosition(0L)
            .setLength(target.bytes)
            .build()
        val buffer = ByteArray(BUFFER_SIZE)
        var totalRead = 0L
        try {
            dataSource.open(dataSpec)
            while (totalRead < target.bytes) {
                val read = dataSource.read(buffer, 0, minOf(buffer.size, (target.bytes - totalRead).toInt()))
                if (read == -1) break
                totalRead += read
            }
            AppLog.i(
                TAG,
                "media_prefetch_ready kind=${target.kind} bytes=$totalRead target=${target.bytes} elapsed=${System.currentTimeMillis() - startedAt}ms"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(
                TAG,
                "media_prefetch_failed kind=${target.kind} bytes=$totalRead target=${target.bytes} " +
                    "message=${e.message}"
            )
        } finally {
            runCatching { dataSource.close() }
        }
    }

    private data class PrefetchTarget(
        val kind: String,
        val url: String,
        val bytes: Long
    )
}
