package com.tutu.myblbl.feature.player

data class PlaybackPreloadTarget(
    val aid: Long? = null,
    val bvid: String? = null,
    val cid: Long,
    val epId: Long? = null,
    val source: Source
) {
    enum class Source {
        NEXT_EPISODE,
        PLAY_QUEUE,
        RELATED_VIDEO
    }
}
