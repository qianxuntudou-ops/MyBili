package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionPreloadVideoWrapper(
    @SerializedName("preload_videos")
    val preloadVideos: List<InteractionPreloadVideoModel>? = null
) : Serializable
