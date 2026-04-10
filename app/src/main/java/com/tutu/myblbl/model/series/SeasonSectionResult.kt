package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.episode.EpisodeModel
import java.io.Serializable

data class SeasonSectionResult(
    @SerializedName("main_section")
    val mainSection: MainEpisodeSection? = null,
    @SerializedName("section")
    val section: List<SectionModel> = emptyList()
) : Serializable

data class MainEpisodeSection(
    @SerializedName("episodes")
    val episodes: List<EpisodeModel> = emptyList(),
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("type")
    val type: Int = 0
) : Serializable
