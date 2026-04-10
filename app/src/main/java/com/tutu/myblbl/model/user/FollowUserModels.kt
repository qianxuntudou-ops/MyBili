package com.tutu.myblbl.model.user

import com.google.gson.annotations.SerializedName

data class GetFollowUserWrapper(
    @SerializedName("list")
    val list: List<FollowingModel>? = null,
    @SerializedName("re_version")
    val reVersion: Int = 0,
    @SerializedName("total")
    val total: Int = 0
)
