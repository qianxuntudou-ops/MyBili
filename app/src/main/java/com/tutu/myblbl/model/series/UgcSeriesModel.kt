package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UgcSeriesModel(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("intro")
    val intro: String = "",
    @SerializedName("sections")
    val sections: List<UgcSectionModel> = emptyList()
) : Serializable
