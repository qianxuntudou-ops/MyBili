package com.tutu.myblbl.model.series.timeline

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SeriesTimeLineModel(
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("delay")
    val delay: Int = 0,
    @SerializedName("delay_reason")
    val delayReason: String = "",
    @SerializedName("ep_cover")
    val epCover: String = "",
    @SerializedName("episode_id")
    val episodeId: Long = 0,
    @SerializedName("pub_index")
    val pubIndex: String = "",
    @SerializedName("pub_time")
    val pubTime: String = "",
    @SerializedName("pub_ts")
    val pubTs: Long = 0,
    @SerializedName("published")
    val published: Int = 0,
    @SerializedName("season_id")
    val seasonId: Long = 0,
    @SerializedName("square_cover")
    val squareCover: String = "",
    @SerializedName("title")
    val title: String = "",
    var dayOfWeek: Int = 0
) : Serializable
