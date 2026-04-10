package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveAreaEntranceWrapper(
    @SerializedName("list")
    val list: List<LiveAreaCategoryParent>? = null
) : Serializable
