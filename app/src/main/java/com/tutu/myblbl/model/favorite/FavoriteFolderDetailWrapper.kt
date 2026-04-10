package com.tutu.myblbl.model.favorite

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.video.HistoryVideoModel
import java.io.Serializable

data class FavoriteFolderDetailWrapper(
    @SerializedName("has_more")
    val hasMore: Boolean = false,
    @SerializedName("info")
    val info: FolderDetailModel? = null,
    @SerializedName("medias")
    val medias: List<HistoryVideoModel> = emptyList()
) : Serializable
