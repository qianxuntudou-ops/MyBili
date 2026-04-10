package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.search.SearchSuggestModel
import java.io.Serializable

data class SearchSuggestWrapper(
    @SerializedName("result")
    val result: SuggestResult = SuggestResult()
) : Serializable

data class SuggestResult(
    @SerializedName("tag")
    val tag: List<SearchSuggestModel> = emptyList()
) : Serializable
