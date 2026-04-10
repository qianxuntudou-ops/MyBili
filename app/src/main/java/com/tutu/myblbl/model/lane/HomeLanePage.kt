package com.tutu.myblbl.model.lane

import com.tutu.myblbl.model.series.timeline.TimeLineADayModel

data class HomeLanePage(
    val sections: List<HomeLaneSection> = emptyList(),
    val nextCursor: Long = 0,
    val hasMore: Boolean = false
)

data class HomeLaneSection(
    val title: String = "",
    val items: List<LaneItemModel> = emptyList(),
    val headers: List<HomeLaneHeader> = emptyList(),
    val timelineDays: List<TimeLineADayModel> = emptyList(),
    val moreSeasonType: Int? = null,
    val moreUrl: String = "",
    val style: String = "",
    val moduleId: Long = 0,
    val disableMore: Boolean = false
)

data class HomeLaneHeader(
    val title: String = "",
    val url: String = ""
)
