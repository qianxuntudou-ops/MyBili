package com.tutu.myblbl.model.lane

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LaneHeaderModel(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("url")
    val url: String = ""
) : Serializable
