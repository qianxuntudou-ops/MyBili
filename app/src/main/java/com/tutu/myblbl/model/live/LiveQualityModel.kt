package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveQualityModel(
    @SerializedName("qn")
    val qn: Int = 0,
    @SerializedName("desc")
    val desc: String = ""
) : Serializable
