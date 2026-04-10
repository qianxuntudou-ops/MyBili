package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveRoomItem(
    @SerializedName("roomid")
    val roomId: Long = 0,
    @SerializedName("uid")
    val uid: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("user_cover")
    val userCover: String = "",
    @SerializedName("keyframe")
    val keyframe: String = "",
    @SerializedName("face")
    val face: String = "",
    @SerializedName("uname")
    val uname: String = "",
    @SerializedName("online")
    val online: Int = 0,
    @SerializedName("area_name")
    val areaName: String = "",
    @SerializedName("area_v2_name")
    val areaV2Name: String = "",
    @SerializedName("parent_area_name")
    val parentAreaName: String = "",
    @SerializedName("area_v2_id")
    val areaV2Id: Int = 0,
    @SerializedName("parent_area_id")
    val parentAreaId: Int = 0,
    @SerializedName("live_status")
    val liveStatus: Int = 0,
    @SerializedName("live_time")
    val liveTime: String = "",
    @SerializedName("watched_show")
    val watchedShow: WatchedShowModel? = null
) : Serializable {
    data class WatchedShowModel(
        @SerializedName("num")
        val num: Int = 0,
        @SerializedName("text_small")
        val textSmall: String = "",
        @SerializedName("text_large")
        val textLarge: String = ""
    ) : Serializable
}
