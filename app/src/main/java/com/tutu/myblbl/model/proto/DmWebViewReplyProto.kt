package com.tutu.myblbl.model.proto

import java.io.Serializable

data class DmWebViewReplyProto(
    val segmentDurationMs: Int = 0,
    val totalSegments: Int = 0,
    val totalCount: Long = 0L,
    val specialDanmakuUrls: List<String> = emptyList()
) : Serializable
