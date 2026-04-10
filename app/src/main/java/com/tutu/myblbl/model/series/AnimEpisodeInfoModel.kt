package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AnimEpisodeInfoModel(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("long_title")
    val longTitle: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("pub_time")
    val pubTime: String = "",
    @SerializedName("index_show")
    val indexShow: String = "",
    @SerializedName("duration")
    val duration: Long = 0
) : Serializable
