package com.tutu.myblbl.utils

object PlaybackReturnStore {
    data class UgcPlaybackEvent(
        val aid: Long,
        val cid: Long,
        val progressMs: Long
    )

    private var pendingUgcPlaybackEvent: UgcPlaybackEvent? = null

    fun recordUgcPlaybackEvent(aid: Long, cid: Long, progressMs: Long) {
        if (aid <= 0L) {
            return
        }
        pendingUgcPlaybackEvent = UgcPlaybackEvent(
            aid = aid,
            cid = cid,
            progressMs = progressMs.coerceAtLeast(0L)
        )
    }

    fun consumeUgcPlaybackEvent(): UgcPlaybackEvent? {
        val event = pendingUgcPlaybackEvent
        pendingUgcPlaybackEvent = null
        return event
    }
}
