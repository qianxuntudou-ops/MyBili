package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionVariableModel(
    @SerializedName("id_v2")
    val idV2: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("value")
    var value: Float = 0f,
    @SerializedName("is_show")
    val isShow: Int = 0
) : Serializable
