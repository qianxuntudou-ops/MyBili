package com.tutu.myblbl.model.video

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LaterWatchWrapper(
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("list")
    val list: List<VideoModel> = emptyList()
) : Serializable
