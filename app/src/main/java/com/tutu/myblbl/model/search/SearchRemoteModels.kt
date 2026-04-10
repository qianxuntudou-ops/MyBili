package com.tutu.myblbl.model.search

import com.google.gson.annotations.SerializedName

data class SearchHotWordItem(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("show_name")
    val showName: String = "",
    @SerializedName("word")
    val word: String = "",
    @SerializedName("score")
    val score: Long = 0,
    @SerializedName("icon")
    val icon: String = ""
)

data class SearchSuggestResponse(
    @SerializedName("tag")
    val tag: List<SearchSuggestItem> = emptyList()
)

data class SearchSuggestItem(
    @SerializedName("value")
    val value: String = "",
    @SerializedName("name")
    val name: String = ""
)
