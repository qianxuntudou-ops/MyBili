package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SeriesUserState(
    @SerializedName("follow")
    val follow: Int = 0,
    @SerializedName("follow_status")
    val followStatus: Int = 0,
    @SerializedName("progress")
    val progress: EpisodeProgressModel? = null
) : Serializable
