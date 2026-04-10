package com.tutu.myblbl.model.video

import com.google.gson.annotations.SerializedName

data class UserDynamicResponse(
    @SerializedName("archives")
    private val archivesData: List<VideoModel>? = null,
    @SerializedName("list")
    private val listData: UserDynamicList? = null,
    @SerializedName("page")
    private val page: UserDynamicPage? = null,
    @SerializedName("has_more")
    private val hasMoreData: Boolean? = null
){
    val archives: List<VideoModel>
        get() = archivesData ?: listData?.vlist.orEmpty()

    val hasMore: Boolean
        get() = hasMoreData ?: page?.let { it.pageNumber * it.pageSize < it.totalCount } ?: false

    val totalCount: Int
        get() = page?.totalCount ?: archives.size
}

data class UserDynamicList(
    @SerializedName("vlist")
    val vlist: List<VideoModel> = emptyList()
)

data class UserDynamicPage(
    @SerializedName("count")
    val totalCount: Int = 0,
    @SerializedName("pn")
    val pageNumber: Int = 1,
    @SerializedName("ps")
    val pageSize: Int = 0
)

data class AllDynamicResponse(
    @SerializedName("items")
    val items: List<VideoModel>? = null,
    @SerializedName("has_more")
    val hasMore: Boolean = false,
    @SerializedName("offset")
    val offset: Long = 0
)
