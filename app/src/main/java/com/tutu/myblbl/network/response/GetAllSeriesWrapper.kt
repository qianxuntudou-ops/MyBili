package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.lane.LaneItemModel
import java.io.Serializable

data class GetAllSeriesWrapper(
    @SerializedName("list")
    val list: List<LaneItemModel> = emptyList()
) : Serializable
