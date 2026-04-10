package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.episode.EpisodeModel
import com.tutu.myblbl.model.episode.NewEpisodeInfoModel
import com.tutu.myblbl.model.episode.EpisodeStatModel
import java.io.Serializable

data class EpisodesDetailModel(
    @SerializedName("media_id")
    val mediaId: Long = 0,
    @SerializedName("season_id")
    val seasonId: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("subtitle")
    val subtitle: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("square_cover")
    val squareCover: String = "",
    @SerializedName("evaluate")
    val evaluate: String = "",
    @SerializedName("season_title")
    val seasonTitle: String = "",
    @SerializedName("share_url")
    val shareUrl: String = "",
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName(value = "type", alternate = ["season_type"])
    val type: Int = 0,
    @SerializedName("episodes")
    val episodes: List<EpisodeModel>? = null,
    @SerializedName("seasons")
    val seasons: List<SeriesModel>? = null,
    @SerializedName("section")
    val section: List<SectionModel>? = null,
    val mainSectionTitle: String = "",
    @SerializedName("stat")
    val stat: EpisodeStatModel? = null,
    @SerializedName("rating")
    val rating: EpisodesRatingModel? = null,
    @SerializedName("new_ep")
    val newEp: NewEpisodeInfoModel? = null,
    @SerializedName("user_status")
    val userStatus: SeriesUserState? = null
) : Serializable
