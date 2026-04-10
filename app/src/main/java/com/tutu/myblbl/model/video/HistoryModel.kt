package com.tutu.myblbl.model.video

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class HistoryModel(
    @SerializedName("oid")
    val oid: Long = 0,
    @SerializedName("epid")
    val epid: Long = 0,
    @SerializedName("bvid")
    val bvid: String = "",
    @SerializedName("page")
    val page: Int = 0,
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("part")
    val part: String = "",
    @SerializedName("business")
    val business: String = "",
    @SerializedName("dt")
    val dt: Int = 0
) : Serializable
