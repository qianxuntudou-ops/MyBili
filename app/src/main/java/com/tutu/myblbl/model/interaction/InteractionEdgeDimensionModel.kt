package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionEdgeDimensionModel(
    @SerializedName("width")
    val width: Int = 0,
    @SerializedName("height")
    val height: Int = 0,
    @SerializedName("rotate")
    val rotate: Int = 0
) : Serializable
