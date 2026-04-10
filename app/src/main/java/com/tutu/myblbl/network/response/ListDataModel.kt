package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName

data class ListDataModel<T>(
    @SerializedName("list")
    val list: List<T> = emptyList(),
    @SerializedName("no_more")
    val noMore: Boolean = false
)
