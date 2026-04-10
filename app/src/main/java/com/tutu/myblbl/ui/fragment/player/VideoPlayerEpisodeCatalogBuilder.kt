package com.tutu.myblbl.ui.fragment.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.tutu.myblbl.model.episode.EpisodeModel
import com.tutu.myblbl.model.episode.EpisodeStatModel
import com.tutu.myblbl.model.series.EpisodesDetailModel
import com.tutu.myblbl.model.video.VideoPvModel
import com.tutu.myblbl.model.video.detail.VideoDetailModel
import com.tutu.myblbl.network.api.ApiService

/**
 * Converts UGC / PGC detail payloads into the episode catalog shape consumed by the player UI.
 */
@OptIn(UnstableApi::class)
class VideoPlayerEpisodeCatalogBuilder(
    private val apiService: ApiService
) {

    suspend fun buildUgcEpisodes(
        detail: VideoDetailModel
    ): List<VideoPlayerViewModel.PlayableEpisode> {
        val view = detail.view ?: return emptyList()

        val ugcEpisodes = view.ugcSeason?.sections
            .orEmpty()
            .flatMap { it.episodes.orEmpty() }
            .map { episode ->
                VideoPlayerViewModel.PlayableEpisode(
                    cid = episode.displayCid,
                    title = episode.displayTitle,
                    subtitle = "第 ${episode.displayPage.coerceAtLeast(1)} P",
                    cover = episode.displayCover.takeIf { it.isNotBlank() } ?: view.pic,
                    aid = episode.displayAid,
                    bvid = episode.displayBvid
                )
            }

        if (ugcEpisodes.isNotEmpty()) {
            return ugcEpisodes
        }

        val pages = view.pages.orEmpty().ifEmpty {
            runCatching { apiService.getVideoPv(view.aid, view.bvid) }
                .getOrNull()
                ?.data
                .orEmpty()
        }

        return pages.mapIndexed { index, page ->
            page.toPlayableEpisode(index, view)
        }
    }

    fun buildPgcEpisodes(
        detail: EpisodesDetailModel
    ): List<VideoPlayerViewModel.PlayableEpisode> {
        val seasonId = detail.seasonId
        val mainEpisodes = detail.episodes.orEmpty().mapIndexed { index, episode ->
            episode.toPlayableEpisode(index, detail.cover, seasonId)
        }
        if (mainEpisodes.isNotEmpty()) {
            return mainEpisodes
        }
        return detail.section.orEmpty()
            .flatMap { section -> section.episodes }
            .mapIndexed { index, episode ->
                VideoPlayerViewModel.PlayableEpisode(
                    cid = episode.cid,
                    title = episode.title.ifBlank { "EP${index + 1}" },
                    subtitle = episode.desc.ifBlank { episode.typeName },
                    cover = episode.coverUrl.ifBlank { detail.cover },
                    aid = episode.aid,
                    bvid = episode.bvid,
                    epId = episode.epid,
                    seasonId = episode.sid.takeIf { it > 0L } ?: seasonId
                )
            }
    }

    fun resolvePgcEpisodeIndex(
        episodes: List<VideoPlayerViewModel.PlayableEpisode>,
        targetEpId: Long?,
        targetCid: Long,
        targetBvid: String?,
        fallbackIndex: Int
    ): Int {
        if (episodes.isEmpty()) {
            return 0
        }
        return episodes.indexOfFirst { episode ->
            (targetEpId != null && targetEpId > 0L && episode.epId == targetEpId) ||
                (targetCid > 0L && episode.cid == targetCid) ||
                (!targetBvid.isNullOrBlank() && episode.bvid == targetBvid)
        }.takeIf { it >= 0 }
            ?: fallbackIndex.takeIf { it in episodes.indices }
            ?: 0
    }

    fun buildPgcVideoDetail(
        detail: EpisodesDetailModel,
        selectedEpisode: VideoPlayerViewModel.PlayableEpisode?,
        fallbackAid: Long,
        fallbackBvid: String,
        fallbackCid: Long
    ): VideoDetailModel {
        return VideoDetailModel(
            view = com.tutu.myblbl.model.video.detail.VideoView(
                aid = selectedEpisode?.aid ?: fallbackAid,
                bvid = selectedEpisode?.bvid ?: fallbackBvid,
                cid = selectedEpisode?.cid ?: fallbackCid,
                pic = detail.cover.ifBlank { selectedEpisode?.cover.orEmpty() },
                title = detail.title.ifBlank {
                    detail.seasonTitle.ifBlank { selectedEpisode?.title.orEmpty() }
                },
                desc = detail.evaluate.ifBlank { detail.subtitle },
                pubDate = detail.episodes.orEmpty()
                    .firstOrNull { it.id == selectedEpisode?.epId }
                    ?.pubTime
                    ?: 0L,
                stat = detail.stat.toVideoStat()
            )
        )
    }

    private fun VideoPvModel.toPlayableEpisode(
        index: Int,
        view: com.tutu.myblbl.model.video.detail.VideoView
    ): VideoPlayerViewModel.PlayableEpisode {
        return VideoPlayerViewModel.PlayableEpisode(
            cid = cid,
            title = part.takeIf { it.isNotBlank() } ?: "P${page.takeIf { it > 0 } ?: index + 1}",
            subtitle = "第 ${page.takeIf { it > 0 } ?: index + 1} P",
            cover = view.pic,
            aid = view.aid,
            bvid = view.bvid
        )
    }

    private fun EpisodeModel.toPlayableEpisode(
        index: Int,
        seasonCover: String,
        seasonId: Long
    ): VideoPlayerViewModel.PlayableEpisode {
        val displayTitle = longTitle.takeIf { it.isNotBlank() }
            ?: title.takeIf { it.isNotBlank() }
            ?: "EP${index + 1}"
        val subTitle = buildList {
            title.takeIf { it.isNotBlank() && it != displayTitle }?.let(::add)
            subtitle.takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(" · ")
        return VideoPlayerViewModel.PlayableEpisode(
            cid = cid,
            title = displayTitle,
            subtitle = subTitle,
            cover = cover.ifBlank { seasonCover },
            aid = aid,
            bvid = bvid,
            epId = id,
            seasonId = seasonId
        )
    }

    private fun EpisodeStatModel?.toVideoStat(): com.tutu.myblbl.model.video.Stat? {
        val value = this ?: return null
        return com.tutu.myblbl.model.video.Stat(
            view = value.view.takeIf { it > 0L } ?: value.play,
            danmaku = value.danmaku,
            reply = value.reply,
            share = value.share,
            coin = value.coin
        )
    }
}
