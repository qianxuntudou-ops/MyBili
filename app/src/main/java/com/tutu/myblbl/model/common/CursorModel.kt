package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CursorModel(
    @SerializedName("max")
    val max: Long = 0,
    @SerializedName("view_at")
    val viewAt: Long = 0,
    @SerializedName("business")
    val business: String = "",
    @SerializedName("ps")
    val ps: Int = 0
) : Serializable
