package com.tutu.myblbl.model.live

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveAreaCategory(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("parent_id")
    val parentId: Long = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("area_type")
    val areaType: Int = 0,
    @SerializedName("pic")
    val pic: String = "",
    @SerializedName("parent_name")
    val parentName: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("link")
    val link: String = "",
    @SerializedName("area_v2_id")
    val areaV2Id: Long = 0,
    @SerializedName("area_v2_parent_id")
    val areaV2ParentId: Long = 0
) : Serializable
