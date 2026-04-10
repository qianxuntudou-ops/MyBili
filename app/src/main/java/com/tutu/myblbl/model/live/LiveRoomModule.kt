package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveRoomModule(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("title")
    val title: String = ""
) : Serializable
