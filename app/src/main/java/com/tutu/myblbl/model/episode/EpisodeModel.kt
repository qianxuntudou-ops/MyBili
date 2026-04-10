package com.tutu.myblbl.model.episode

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.series.BadgeInfoModel
import java.io.Serializable

data class EpisodeModel(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("aid")
    val aid: Long = 0,
    @SerializedName("bvid")
    val bvid: String = "",
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("long_title")
    val longTitle: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("badge")
    val badge: String = "",
    @SerializedName("badge_info")
    val badgeInfo: BadgeInfoModel? = null,
    @SerializedName("link")
    val link: String = "",
    @SerializedName("share_url")
    val shareUrl: String = "",
    @SerializedName("short_link")
    val shortLink: String = "",
    @SerializedName("subtitle")
    val subtitle: String = "",
    @SerializedName("vid")
    val vid: String = "",
    @SerializedName("pub_time")
    val pubTime: Long = 0
) : Serializable
