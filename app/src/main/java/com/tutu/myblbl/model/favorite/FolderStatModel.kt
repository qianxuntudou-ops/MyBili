package com.tutu.myblbl.model.favorite

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FolderStatModel(
    @SerializedName("collect")
    val collect: Long = 0,
    @SerializedName("danmaku")
    val danmaku: Long = 0,
    @SerializedName("play")
    val play: Long = 0,
    @SerializedName("share")
    val share: Long = 0,
    @SerializedName("thumb_up")
    val thumbUp: Long = 0
) : Serializable
