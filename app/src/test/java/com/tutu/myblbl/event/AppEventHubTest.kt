package com.tutu.myblbl.event

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AppEventHubTest {

    @Test
    fun dispatch_emits_typed_events_in_order() = runBlocking {
        val eventHub = AppEventHub()
        val events = mutableListOf<AppEventHub.Event>()

        val collectionJob = launch(start = CoroutineStart.UNDISPATCHED) {
            eventHub.events.take(3).toList(events)
        }

        eventHub.dispatch(AppEventHub.Event.UserSessionChanged)
        eventHub.dispatch(
            AppEventHub.Event.PlaybackProgressUpdated(
                aid = 1L,
                cid = 2L,
                progressMs = 3L
            )
        )
        eventHub.dispatch(
            AppEventHub.Event.EpisodePlaybackProgressUpdated(
                episodeId = 4L,
                progressMs = 5L,
                episodeIndex = "EP1"
            )
        )

        withTimeout(1_000L) {
            collectionJob.join()
        }

        assertEquals(
            listOf(
                AppEventHub.Event.UserSessionChanged,
                AppEventHub.Event.PlaybackProgressUpdated(
                    aid = 1L,
                    cid = 2L,
                    progressMs = 3L
                ),
                AppEventHub.Event.EpisodePlaybackProgressUpdated(
                    episodeId = 4L,
                    progressMs = 5L,
                    episodeIndex = "EP1"
                )
            ),
            events
        )
    }
}
