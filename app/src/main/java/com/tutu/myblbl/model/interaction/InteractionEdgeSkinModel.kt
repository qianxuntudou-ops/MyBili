package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionEdgeSkinModel(
    @SerializedName("progress_color")
    val progressColor: String = "",
    @SerializedName("text_color")
    val textColor: String = ""
) : Serializable
