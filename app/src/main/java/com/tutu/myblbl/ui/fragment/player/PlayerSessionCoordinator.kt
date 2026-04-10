package com.tutu.myblbl.ui.fragment.player

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.model.video.detail.VideoDetailModel
import com.tutu.myblbl.utils.serializableCompat
import java.util.ArrayDeque

@OptIn(UnstableApi::class)
class PlayerSessionCoordinator {

    sealed class ContinuationPlan {
        data class PlayNextEpisode(
            val title: String,
            val coverUrl: String,
            val perform: () -> Unit
        ) : ContinuationPlan()

        data class PlayVideo(
            val title: String,
            val coverUrl: String,
            val perform: () -> Unit
        ) : ContinuationPlan()

        object ExitPlayer : ContinuationPlan()

        object ShowController : ContinuationPlan()
    }

    private var launchContext: PlayerLaunchContext? = null
    private var launchVideo: VideoModel? = null
    private var launchStartEpisodeIndex: Int = -1
    private val launchQueue = ArrayDeque<VideoModel>()

    private var episodes: List<VideoPlayerViewModel.PlayableEpisode> = emptyList()
    private var selectedEpisodeIndex: Int = 0
    private var relatedVideos: List<VideoModel> = emptyList()

    fun consumeLaunchContext(arguments: Bundle?): PlayerLaunchContext? {
        val resolved = resolveLaunchContext(arguments) ?: return null
        launchContext = resolved
        launchVideo = resolved.initialVideo
        launchStartEpisodeIndex = resolved.startEpisodeIndex
        launchQueue.clear()
        launchQueue.addAll(resolved.playQueue.filter(::isPlayableVideo))
        trimQueueAgainstCurrent(launchVideo)
        if (launchStartEpisodeIndex >= 0 && selectedEpisodeIndex == 0) {
            selectedEpisodeIndex = launchStartEpisodeIndex
        }
        return resolved
    }

    fun resolveLaunchContext(arguments: Bundle?): PlayerLaunchContext? {
        val args = arguments ?: return null
        args.serializableCompat<PlayerLaunchContext>(VideoPlayerFragment.ARG_LAUNCH_CONTEXT)?.let {
            return it
        }
        return PlayerLaunchContext.create(
            aid = args.getLong(VideoPlayerFragment.ARG_AID, 0L),
            bvid = args.getString(VideoPlayerFragment.ARG_BVID).orEmpty(),
            cid = args.getLong(VideoPlayerFragment.ARG_CID, 0L),
            epId = args.getLong(VideoPlayerFragment.ARG_EP_ID, 0L),
            seasonId = args.getLong(VideoPlayerFragment.ARG_SEASON_ID, 0L),
            seekPositionMs = args.getLong(VideoPlayerFragment.ARG_SEEK_POSITION_MS, 0L),
            initialVideo = args.serializableCompat(VideoPlayerFragment.ARG_INITIAL_VIDEO),
            playQueue = args.serializableCompat<ArrayList<VideoModel>>(VideoPlayerFragment.ARG_PLAY_QUEUE).orEmpty(),
            startEpisodeIndex = args.getInt(VideoPlayerFragment.ARG_START_EPISODE, -1)
        )
    }

    fun updateVideoInfo(info: VideoDetailModel?) {
        trimQueueAgainstCurrent(info?.view?.toLaunchVideoModel())
    }

    fun updateEpisodes(value: List<VideoPlayerViewModel.PlayableEpisode>) {
        episodes = value
    }

    fun updateSelectedEpisodeIndex(index: Int) {
        selectedEpisodeIndex = index
    }

    fun updateRelatedVideos(value: List<VideoModel>) {
        relatedVideos = value
    }

    fun replacePlayQueue(playQueue: List<VideoModel>) {
        launchQueue.clear()
        launchQueue.addAll(playQueue.filter(::isPlayableVideo))
    }

    fun getLaunchVideo(): VideoModel? = launchVideo

    fun getEpisodes(): List<VideoPlayerViewModel.PlayableEpisode> = episodes

    fun getSelectedEpisodeIndex(): Int = selectedEpisodeIndex

    fun getSelectedEpisode(): VideoPlayerViewModel.PlayableEpisode? = episodes.getOrNull(selectedEpisodeIndex)

    fun buildContinuationPlan(
        continuePlaybackAfterFinish: Boolean,
        exitPlayerWhenPlaybackFinished: Boolean,
        hasNextEpisode: Boolean,
        nextEpisode: VideoPlayerViewModel.PlayableEpisode?,
        playNextEpisode: () -> Unit,
        playVideo: (VideoModel) -> Unit
    ): ContinuationPlan {
        if (!continuePlaybackAfterFinish) {
            return if (exitPlayerWhenPlaybackFinished) {
                ContinuationPlan.ExitPlayer
            } else {
                ContinuationPlan.ShowController
            }
        }
        if (hasNextEpisode && nextEpisode != null) {
            return ContinuationPlan.PlayNextEpisode(
                title = nextEpisode.title,
                coverUrl = nextEpisode.cover,
                perform = playNextEpisode
            )
        }
        val queuedVideo = launchQueue.pollFirst()
        if (queuedVideo != null) {
            return ContinuationPlan.PlayVideo(
                title = queuedVideo.title,
                coverUrl = queuedVideo.coverUrl,
                perform = { playVideo(queuedVideo) }
            )
        }
        val related = relatedVideos.firstOrNull()
        if (related != null) {
            return ContinuationPlan.PlayVideo(
                title = related.title,
                coverUrl = related.coverUrl,
                perform = { playVideo(related) }
            )
        }
        return if (exitPlayerWhenPlaybackFinished) {
            ContinuationPlan.ExitPlayer
        } else {
            ContinuationPlan.ShowController
        }
    }

    private fun trimQueueAgainstCurrent(current: VideoModel?) {
        val currentVideo = current ?: return
        while (launchQueue.peekFirst()?.let { isSameVideo(it, currentVideo) } == true) {
            launchQueue.removeFirst()
        }
    }

    private fun isPlayableVideo(video: VideoModel): Boolean {
        return video.aid > 0L ||
            video.bvid.isNotBlank() ||
            video.epid > 0L ||
            video.playbackSeasonId > 0L
    }

    private fun isSameVideo(left: VideoModel, right: VideoModel): Boolean {
        return when {
            left.epid > 0L && right.epid > 0L -> left.epid == right.epid
            left.bvid.isNotBlank() && right.bvid.isNotBlank() -> left.bvid == right.bvid
            left.aid > 0L && right.aid > 0L -> left.aid == right.aid
            left.cid > 0L && right.cid > 0L -> left.cid == right.cid
            else -> left.title == right.title && left.coverUrl == right.coverUrl
        }
    }

    private fun com.tutu.myblbl.model.video.detail.VideoView.toLaunchVideoModel(): VideoModel {
        return VideoModel(
            aid = aid,
            bvid = bvid,
            title = title,
            pic = pic,
            cid = cid,
            desc = desc,
            duration = duration,
            pubDate = pubDate,
            owner = owner,
            stat = stat
        )
    }
}
