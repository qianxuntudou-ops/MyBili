package com.tutu.myblbl.model.episode

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EpisodeStatModel(
    @SerializedName(value = "coins", alternate = ["coin"])
    private val coins: Long = 0,
    @SerializedName(value = "danmakus", alternate = ["danmaku"])
    private val danmakus: Long = 0,
    @SerializedName("favorites")
    val favorites: Long = 0,
    @SerializedName("likes")
    val likes: Long = 0,
    @SerializedName("reply")
    val reply: Long = 0,
    @SerializedName("share")
    val share: Long = 0,
    @SerializedName(value = "views", alternate = ["view", "play"])
    private val views: Long = 0
) : Serializable {
    val coin: Long
        get() = coins

    val danmaku: Long
        get() = danmakus

    val play: Long
        get() = views

    val view: Long
        get() = views
}
