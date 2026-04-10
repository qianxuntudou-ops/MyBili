package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveRoomWrapper(
    @SerializedName("list")
    val list: List<LiveRoomItem>? = null,
    @SerializedName("module_info")
    val moduleInfo: LiveRoomModule? = null
) : Serializable
