package com.tutu.myblbl.model.favorite

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CheckFavoriteModel(
    @SerializedName("favoured")
    var favoured: Boolean = false
) : Serializable
