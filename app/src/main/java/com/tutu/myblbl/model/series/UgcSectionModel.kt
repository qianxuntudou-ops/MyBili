package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UgcSectionModel(
    @SerializedName("episodes")
    val episodes: List<UgcEpisodeModel> = emptyList(),
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("type")
    val type: Int = 0
) : Serializable
