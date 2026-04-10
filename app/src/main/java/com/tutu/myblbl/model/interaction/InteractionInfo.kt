package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionInfo(
    @SerializedName("graph_version")
    val graphVersion: Long = 0,
    @SerializedName("mark")
    val mark: Int = 0
) : Serializable
