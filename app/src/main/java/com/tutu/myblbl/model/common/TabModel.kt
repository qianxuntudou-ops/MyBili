package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TabModel(
    @SerializedName("type")
    val type: String = "",
    @SerializedName("name")
    val name: String = ""
) : Serializable
