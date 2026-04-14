package com.tutu.myblbl.model.episode

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class NewEpisodeInfoModel(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("desc")
    val desc: String = "",
    @SerializedName("long_title")
    val longTitle: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("index_show")
    val indexShow: String = "",
    @SerializedName("pub_time")
    val pubTime: String = ""
) : Serializable
