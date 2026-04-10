package com.tutu.myblbl.model.user

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OwnerModel(
    @SerializedName("mid")
    var mid: Long = 0,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("face")
    var face: String = "",
    @SerializedName("sign")
    var sign: String = "",
    @SerializedName("level")
    var level: Int = 0,
    @SerializedName("sex")
    var sex: String = ""
) : Serializable
