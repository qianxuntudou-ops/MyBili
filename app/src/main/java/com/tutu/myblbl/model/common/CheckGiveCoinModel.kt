package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CheckGiveCoinModel(
    @SerializedName("multiply")
    val multiply: Int = 0
) : Serializable
