package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.video.VideoModel
import java.io.Serializable

data class UgcEpisodeModel(
    @SerializedName("aid")
    val aid: Long = 0,
    @SerializedName("arc")
    val arc: VideoModel? = null,
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("bvid")
    val bvid: String = "",
    @SerializedName("title")
    val title: String = ""
) : Serializable
