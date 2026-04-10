package com.tutu.myblbl.model.series.timeline

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TimeLineADayModel(
    @SerializedName("date")
    val date: String = "",
    @SerializedName("date_ts")
    val dateTs: Long = 0,
    @SerializedName("day_of_week")
    val dayOfWeek: Int = 0,
    @SerializedName("episodes")
    val episodes: List<SeriesTimeLineModel> = emptyList(),
    @SerializedName("is_today")
    val isToday: Int = 0
) : Serializable
