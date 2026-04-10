package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveAreaCategoryParent(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName(value = "list", alternate = ["area_list"])
    val areaList: List<LiveAreaCategory>? = null
) : Serializable
