package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionEdgeQuestionModel(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("type")
    val type: Int = 0,
    @SerializedName("start_time_r")
    val startTimeR: Long = 0,
    @SerializedName("duration")
    val duration: Long = 0,
    @SerializedName("pause_video")
    val pauseVideo: Int = 0,
    @SerializedName("choices")
    val choices: List<InteractionEdgeQuestionChoiceModel>? = null
) : Serializable
