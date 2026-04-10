package com.tutu.myblbl.model.favorite

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FolderDetailModel(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("fid")
    val fid: Long = 0,
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("cover")
    val cover: String = "",
    @SerializedName("cover_type")
    val coverType: Int = 0,
    @SerializedName("cnt_info")
    val cntInfo: FolderStatModel? = null,
    @SerializedName("ctime")
    val ctime: Long = 0,
    @SerializedName("mtime")
    val mtime: Long = 0,
    @SerializedName("media_count")
    val mediaCount: Int = 0
) : Serializable
