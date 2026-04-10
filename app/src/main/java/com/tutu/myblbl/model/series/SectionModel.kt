package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.video.VideoModel
import java.io.Serializable

data class SectionModel(
    @SerializedName("episode_id")
    val episodeId: Long = 0,
    @SerializedName("episodes")
    val episodes: List<VideoModel> = emptyList(),
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("type")
    val type: Int = 0
) : Serializable
