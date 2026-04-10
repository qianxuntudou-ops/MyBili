package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SeriesModel(
    @SerializedName("season_id")
    val seasonId: Long = 0,
    @SerializedName("media_id")
    val mediaId: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("badge")
    val badge: String = "",
    @SerializedName("badge_info")
    val badgeInfo: BadgeInfoModel? = null,
    @SerializedName("season_title")
    val seasonTitle: String = "",
    @SerializedName("evaluate")
    val evaluate: String = "",
    @SerializedName("summary")
    val summary: String = "",
    @SerializedName("url")
    val url: String = "",
    @SerializedName("progress")
    val progress: String = "",
    @SerializedName("horizontal_cover_16_9")
    val horizontalCover169: String = "",
    @SerializedName("first_ep")
    val firstEp: Long = 0,
    @SerializedName("first_ep_info")
    val firstEpInfo: AnimEpisodeInfoModel? = null,
    @SerializedName("new_ep")
    val newEp: AnimEpisodeInfoModel? = null,
    @SerializedName("season_attr")
    val seasonAttr: Long = 0,
    @SerializedName("media_attr")
    val mediaAttr: Long = 0
) : Serializable
