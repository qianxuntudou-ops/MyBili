package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ChatRoomWrapper(
    @SerializedName("group")
    val group: String = "",
    @SerializedName("token")
    val token: String = "",
    @SerializedName("host_list")
    val hostList: List<ChatHostUrlModel>? = null
) : Serializable
