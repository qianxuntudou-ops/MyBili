package com.tutu.myblbl.model.favorite

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FavoriteFolderModel(
    @SerializedName("id")
    var id: Long = 0,
    @SerializedName("fid")
    var fid: Long = 0,
    @SerializedName("mid")
    var mid: Long = 0,
    @SerializedName("attr")
    var attr: Int = 0,
    @SerializedName("title")
    var title: String = "",
    @SerializedName("cover")
    var cover: String = "",
    @SerializedName("fav_state")
    var favState: Int = 0,
    @SerializedName("media_count")
    var mediaCount: Int = 0,
    var imageUrl: String = ""
) : Serializable {
    val displayImageUrl: String
        get() = imageUrl.ifEmpty { cover }
}
