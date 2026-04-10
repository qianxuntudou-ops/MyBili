package com.tutu.myblbl.model.lane

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.series.AnimEpisodeInfoModel
import com.tutu.myblbl.model.series.BadgeInfoModel
import java.io.Serializable

data class LaneItemModel(
    @SerializedName("badge_info")
    val badgeInfo: BadgeInfoModel? = null,
    @SerializedName("link_type")
    val linkType: Int = 0,
    @SerializedName("link_value")
    val linkValue: Int = 0,
    @SerializedName("new_ep")
    val newEp: AnimEpisodeInfoModel? = null,
    @SerializedName("oid")
    val oid: Long = 0,
    @SerializedName("season_id")
    val seasonId: Long = 0,
    @SerializedName("season_type")
    val seasonType: Int = 0,
    @SerializedName("stat")
    val stat: LaneStatModel? = null,
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("desc")
    val desc: String = "",
    @SerializedName("link")
    val link: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("subTitle")
    val subTitle: String = ""
) : Serializable
