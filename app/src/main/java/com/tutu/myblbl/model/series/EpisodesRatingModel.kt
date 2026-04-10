package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EpisodesRatingModel(
    @SerializedName("score")
    val score: Double = 0.0,
    @SerializedName("count")
    val count: Long = 0
) : Serializable
