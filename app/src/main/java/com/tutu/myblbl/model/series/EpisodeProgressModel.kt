package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EpisodeProgressModel(
    @SerializedName("last_ep_id")
    val lastEpId: Long = 0,
    @SerializedName("last_ep_index")
    val lastEpIndex: String = "",
    @SerializedName("last_time")
    val lastTime: Long = 0
) : Serializable
