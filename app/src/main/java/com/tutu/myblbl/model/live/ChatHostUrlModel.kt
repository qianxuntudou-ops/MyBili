package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ChatHostUrlModel(
    @SerializedName("host")
    val host: String = "",
    @SerializedName("port")
    val port: Int = 0,
    @SerializedName("ws_port")
    val wsPort: Int = 0,
    @SerializedName("wss_port")
    val wssPort: Int = 0
) : Serializable
