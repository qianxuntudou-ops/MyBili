package com.tutu.myblbl.model.subtitle

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SubtitleWrapper(
    @SerializedName("subtitles")
    val subtitles: List<SubtitleInfoModel> = emptyList()
) : Serializable
