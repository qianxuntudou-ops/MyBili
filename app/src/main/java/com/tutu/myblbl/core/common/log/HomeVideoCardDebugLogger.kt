package com.tutu.myblbl.core.common.log

import com.tutu.myblbl.model.video.VideoModel

object HomeVideoCardDebugLogger {

    private const val TAG = "HomeVideoCardDiag"
    private const val MAX_SAMPLE_COUNT = 5

    fun logRejectedCards(source: String, items: List<VideoModel>) {
        val rejected = items.filterNot { it.isSupportedHomeVideoCard }
        if (rejected.isEmpty()) {
            return
        }
        AppLog.w(TAG, "source=$source rejected=${rejected.size}/${items.size}")
        rejected
            .take(MAX_SAMPLE_COUNT)
            .forEachIndexed { index, video ->
                AppLog.w(
                    TAG,
                    "source=$source sample#$index reasons=${video.unsupportedHomeVideoReasons().joinToString(",")} " +
                        "title=${video.title.take(30)} aid=${video.aid} bvid=${video.bvid} cid=${video.cid} " +
                        "epId=${video.playbackEpId} seasonId=${video.playbackSeasonId} goto=${video.goto} cover=${video.coverUrl}"
                )
            }
    }
}
