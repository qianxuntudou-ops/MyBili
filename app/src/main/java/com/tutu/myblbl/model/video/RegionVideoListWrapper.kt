package com.tutu.myblbl.model.video

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RegionVideoListWrapper(
    @SerializedName("page")
    val page: RegionPageInfo? = null,
    @SerializedName("archives")
    val archives: List<VideoModel> = emptyList()
) : Serializable

data class RegionPageInfo(
    @SerializedName("num")
    val num: Int = 1,
    @SerializedName("size")
    val size: Int = 0,
    @SerializedName("count")
    val count: Int = 0
) : Serializable
