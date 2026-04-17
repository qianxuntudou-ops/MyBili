package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName

data class RelatedRecommendResult(
    @SerializedName("relates")
    val relates: List<SeriesModel> = emptyList(),
    @SerializedName("season")
    val season: List<SeriesModel> = emptyList()
)
