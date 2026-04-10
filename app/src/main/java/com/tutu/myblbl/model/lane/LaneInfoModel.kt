package com.tutu.myblbl.model.lane

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.series.timeline.TimeLineADayModel
import java.io.Serializable

data class LaneInfoModel(
    @SerializedName("headers")
    val headers: List<LaneHeaderModel> = emptyList(),
    @SerializedName("items")
    val items: List<LaneItemModel> = emptyList(),
    @SerializedName("module_id")
    val moduleId: Long = 0,
    @SerializedName("size")
    val size: Int = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("style")
    val style: String = "",
    val timelineDay: List<TimeLineADayModel> = emptyList()
) : Serializable
