package com.tutu.myblbl.model.series

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MyFollowingResponseWrapper(
    @SerializedName("list")
    val list: List<SeriesModel> = emptyList(),
    @SerializedName("pn")
    val pn: Int = 1,
    @SerializedName("ps")
    val ps: Int = 20,
    @SerializedName("total")
    val total: Int = 0
) : Serializable
