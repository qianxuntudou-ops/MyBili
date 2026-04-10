package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveDUrlModel(
    @SerializedName("url")
    val url: String = "",
    @SerializedName("order")
    val order: Int = 0
) : Serializable
