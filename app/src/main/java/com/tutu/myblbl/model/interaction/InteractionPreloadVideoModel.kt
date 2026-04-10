package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionPreloadVideoModel(
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("preload_code")
    val preloadCode: Int = 0
) : Serializable
