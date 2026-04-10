package com.tutu.myblbl.model.video

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ChannelVideoModel(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("bvid")
    val bvid: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("cover")
    val cover: String = "",

    @SerializedName("duration")
    val duration: String = "",

    @SerializedName("author_id")
    val authorId: String = "",

    @SerializedName("author_name")
    val authorName: String = "",

    @SerializedName("view_count")
    val viewCount: String = "",

    @SerializedName("like_count")
    val likeCount: String = "",

    @SerializedName("danmaku")
    val danmaku: Long = 0
) : Serializable {

    fun toVideoModel(): VideoModel {
        return VideoModel(
            aid = id,
            bvid = bvid,
            title = name,
            pic = cover,
            duration = parseDurationStr(duration),
            owner = Owner(
                mid = authorId.toLongOrNull() ?: 0L,
                name = authorName
            )
        )
    }

    private fun parseDurationStr(value: String): Long {
        if (value.isBlank()) return 0L
        val parts = value.split(":").mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty()) return 0L
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            else -> parts[0]
        }
    }
}

data class GetVideoByChannelWrapper(
    @SerializedName("list")
    val list: List<ChannelVideoModel>? = null,

    @SerializedName("offset")
    val offset: String = "",

    @SerializedName("has_more")
    val hasMore: Boolean = true
) : Serializable
