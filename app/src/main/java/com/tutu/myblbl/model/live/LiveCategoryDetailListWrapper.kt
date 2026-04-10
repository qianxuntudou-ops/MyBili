package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveCategoryDetailListWrapper(
    @SerializedName("list")
    val list: List<LiveRoomItem>? = null,
    @SerializedName("has_more")
    val hasMore: Int = 0
) : Serializable
