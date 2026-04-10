package com.tutu.myblbl.ui.fragment.main.me

import com.tutu.myblbl.model.video.HistoryVideoModel
import com.tutu.myblbl.model.video.VideoModel

data class MeListUiState(
    val historyVideos: List<HistoryVideoModel> = emptyList(),
    val laterVideos: List<VideoModel> = emptyList()
)
