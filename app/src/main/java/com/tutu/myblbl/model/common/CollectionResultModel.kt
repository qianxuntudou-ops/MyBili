package com.tutu.myblbl.model.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CollectionResultModel(
    @SerializedName("prompt")
    val prompt: Boolean = false
) : Serializable
