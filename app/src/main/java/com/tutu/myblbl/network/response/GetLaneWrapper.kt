package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.lane.LaneInfoModel
import java.io.Serializable

data class GetLaneWrapper(
    @SerializedName("has_next")
    val hasNext: Int = 1,
    @SerializedName("modules")
    val modules: List<LaneInfoModel> = emptyList(),
    @SerializedName("next_cursor")
    val nextCursor: Long = 0
) : Serializable
