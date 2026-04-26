package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TripleActionResultModel(
    @SerializedName("coin")
    val coin: Boolean = false,
    @SerializedName("fav")
    val fav: Boolean = false,
    @SerializedName("like")
    val like: Boolean = false,
    @SerializedName("multiply")
    val multiply: Int = 0,
    @SerializedName("is_risk")
    val isRisk: Boolean = false
) : Serializable
