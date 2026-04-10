package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.search.HotWordModel
import java.io.Serializable

data class HotWordWrapper(
    @SerializedName("list")
    val list: List<HotWordModel> = emptyList(),
    @SerializedName("message")
    val message: String = ""
) : Serializable
