package com.tutu.myblbl.model.subtitle

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SubtitleModel(
    @SerializedName("content")
    val content: String = "",
    @SerializedName("from")
    val from: Double = 0.0,
    @SerializedName("to")
    val to: Double = 0.0
) : Serializable
