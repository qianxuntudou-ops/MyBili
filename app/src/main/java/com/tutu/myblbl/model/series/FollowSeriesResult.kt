package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FollowSeriesResult(
    @SerializedName("fmid")
    val fmid: Long = 0,
    @SerializedName("relation")
    val relation: Boolean = false,
    @SerializedName("status")
    val status: Int = 0,
    @SerializedName("toast")
    val toast: String = ""
) : Serializable
