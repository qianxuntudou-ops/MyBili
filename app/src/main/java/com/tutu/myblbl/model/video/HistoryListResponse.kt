package com.tutu.myblbl.model.video

import com.google.gson.annotations.SerializedName
import com.tutu.myblbl.model.common.CursorModel
import com.tutu.myblbl.model.common.TabModel
import java.io.Serializable

data class HistoryListResponse(
    @SerializedName("cursor")
    val cursor: CursorModel? = null,
    @SerializedName("list")
    val list: List<HistoryVideoModel> = emptyList(),
    @SerializedName("tab")
    val tab: List<TabModel> = emptyList()
) : Serializable
