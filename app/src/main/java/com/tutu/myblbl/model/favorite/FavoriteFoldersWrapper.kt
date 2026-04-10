package com.tutu.myblbl.model.favorite

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FavoriteFoldersWrapper(
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("list")
    val list: List<FavoriteFolderModel> = emptyList()
) : Serializable
