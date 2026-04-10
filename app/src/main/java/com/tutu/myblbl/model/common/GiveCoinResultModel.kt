package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class GiveCoinResultModel(
    @SerializedName("like")
    val like: Boolean = false
) : Serializable
