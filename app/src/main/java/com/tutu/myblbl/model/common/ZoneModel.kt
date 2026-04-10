package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ZoneModel(
    @SerializedName("addr")
    val addr: String = "",
    @SerializedName("country")
    val country: String = "",
    @SerializedName("province")
    val province: String = "",
    @SerializedName("city")
    val city: String = "",
    @SerializedName("isp")
    val isp: String = ""
) : Serializable
