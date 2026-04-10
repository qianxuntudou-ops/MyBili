package com.tutu.myblbl.model.lane

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LaneStatModel(
    @SerializedName("danmakus")
    val danmakus: Int = 0,
    @SerializedName("follow")
    val follow: Int = 0,
    @SerializedName("series_follow")
    val seriesFollow: Int = 0,
    @SerializedName("views")
    val views: Int = 0
) : Serializable
