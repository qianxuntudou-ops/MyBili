package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveListWrapper(
    @SerializedName("area_entrance_v2")
    val areaEntranceV2: LiveAreaEntranceWrapper? = null,
    @SerializedName("room_list")
    val roomList: List<LiveRoomWrapper>? = null,
    @SerializedName("recommend_room_list")
    val recommendRoomList: List<LiveRoomItem>? = null
) : Serializable
