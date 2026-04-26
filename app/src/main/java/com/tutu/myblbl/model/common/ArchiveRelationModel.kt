package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName

data class ArchiveRelationModel(
    @SerializedName("attention")
    val attention: Boolean = false,
    @SerializedName("favorite")
    val favorite: Boolean = false,
    @SerializedName("season_fav")
    val seasonFav: Boolean = false,
    @SerializedName("like")
    val like: Boolean = false,
    @SerializedName("dislike")
    val dislike: Boolean = false,
    @SerializedName("coin")
    val coin: Int = 0
)
