package com.tutu.myblbl.model.series.timeline

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class GetTimeLineWrapper(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("result")
    val result: List<TimeLineADayModel> = emptyList()
) : Serializable
