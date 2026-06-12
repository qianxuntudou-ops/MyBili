package com.tutu.myblbl.feature.player

import com.tutu.myblbl.feature.player.settings.AfterPlayMode
import com.tutu.myblbl.model.video.VideoModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSessionCoordinatorPlayQueueTest {

    @Test
    fun playQueueBuildsContinuationForListItemWithoutCid() {
        val coordinator = PlayerSessionCoordinator()
        val next = VideoModel(aid = 2L, bvid = "BV2", title = "next", cid = 0L)
        coordinator.updateCurrentVideo(VideoModel(aid = 1L, bvid = "BV1", title = "current", cid = 101L))
        coordinator.replacePlayQueue(listOf(next))

        val plan = coordinator.buildContinuationPlan(
            afterPlayMode = AfterPlayMode.PLAY_QUEUE,
            exitPlayerWhenPlaybackFinished = false,
            hasNextEpisode = false,
            nextEpisode = null
        )

        assertTrue(plan is PlayerSessionCoordinator.ContinuationPlan.PlayIntent)
        val intent = (plan as PlayerSessionCoordinator.ContinuationPlan.PlayIntent).intent
        assertEquals(ContinuationPlaybackIntent.Kind.PLAY_QUEUE, intent.kind)
        assertEquals(2L, intent.target.aid)
        assertEquals("BV2", intent.target.bvid)
        assertEquals(0L, intent.target.cid)
    }

    @Test
    fun playQueueSkipsBlockedVideosBeforeBuildingContinuation() {
        val coordinator = PlayerSessionCoordinator()
        val blocked = VideoModel(aid = 2L, bvid = "BV2", title = "blocked")
        val allowed = VideoModel(aid = 3L, bvid = "BV3", title = "allowed")
        coordinator.setContentGate(
            isVideoAllowed = { it.aid != blocked.aid },
            isEpisodeAllowed = { true }
        )
        coordinator.updateCurrentVideo(VideoModel(aid = 1L, bvid = "BV1", title = "current"))
        coordinator.replacePlayQueue(listOf(blocked, allowed))

        val plan = coordinator.buildContinuationPlan(
            afterPlayMode = AfterPlayMode.PLAY_QUEUE,
            exitPlayerWhenPlaybackFinished = false,
            hasNextEpisode = false,
            nextEpisode = null
        )

        assertTrue(plan is PlayerSessionCoordinator.ContinuationPlan.PlayIntent)
        val intent = (plan as PlayerSessionCoordinator.ContinuationPlan.PlayIntent).intent
        assertEquals(ContinuationPlaybackIntent.Kind.PLAY_QUEUE, intent.kind)
        assertEquals(3L, intent.target.aid)
    }

    @Test
    fun recommendContinuationIgnoresBlockedRelatedVideo() {
        val coordinator = PlayerSessionCoordinator()
        val blocked = VideoModel(aid = 2L, bvid = "BV2", title = "blocked")
        val allowed = VideoModel(aid = 3L, bvid = "BV3", title = "allowed")
        coordinator.setContentGate(
            isVideoAllowed = { it.aid != blocked.aid },
            isEpisodeAllowed = { true }
        )
        coordinator.updateCurrentVideo(VideoModel(aid = 1L, bvid = "BV1", title = "current"))
        coordinator.updateRelatedVideos(listOf(blocked, allowed))

        val plan = coordinator.buildContinuationPlan(
            afterPlayMode = AfterPlayMode.RECOMMEND,
            exitPlayerWhenPlaybackFinished = false,
            hasNextEpisode = false,
            nextEpisode = null
        )

        assertTrue(plan is PlayerSessionCoordinator.ContinuationPlan.PlayIntent)
        val intent = (plan as PlayerSessionCoordinator.ContinuationPlan.PlayIntent).intent
        assertEquals(ContinuationPlaybackIntent.Kind.RECOMMEND, intent.kind)
        assertEquals(3L, intent.target.aid)
    }

    @Test
    fun blockedNextEpisodeDoesNotBuildContinuation() {
        val coordinator = PlayerSessionCoordinator()
        val nextEpisode = VideoPlayerViewModel.PlayableEpisode(
            cid = 20L,
            title = "blocked",
            aid = 2L,
            bvid = "BV2"
        )
        coordinator.setContentGate(
            isVideoAllowed = { true },
            isEpisodeAllowed = { it.aid != nextEpisode.aid }
        )

        val plan = coordinator.buildContinuationPlan(
            afterPlayMode = AfterPlayMode.NEXT_EPISODE,
            exitPlayerWhenPlaybackFinished = false,
            hasNextEpisode = true,
            nextEpisode = nextEpisode
        )

        assertTrue(plan is PlayerSessionCoordinator.ContinuationPlan.ShowController)
    }

    @Test
    fun canPlayEpisodeUsesContentGateWithoutChangingEpisodeIndexes() {
        val coordinator = PlayerSessionCoordinator()
        val allowed = VideoPlayerViewModel.PlayableEpisode(cid = 10L, title = "allowed", aid = 1L)
        val blocked = VideoPlayerViewModel.PlayableEpisode(cid = 20L, title = "blocked", aid = 2L)
        coordinator.setContentGate(
            isVideoAllowed = { true },
            isEpisodeAllowed = { it.aid != blocked.aid }
        )

        coordinator.updateEpisodes(listOf(allowed, blocked))

        assertEquals(listOf(allowed, blocked), coordinator.getEpisodes())
        assertTrue(coordinator.canPlayEpisode(0))
        assertTrue(!coordinator.canPlayEpisode(1))
    }
}
