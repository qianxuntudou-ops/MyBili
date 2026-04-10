package com.tutu.myblbl.model.recommend

import com.google.gson.annotations.SerializedName

data class RecommendListDataModel<T>(
    @SerializedName("item")
    val items: List<T>? = null,

    @SerializedName("mid")
    val mid: Long = 0
)
