package com.tutu.myblbl.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutu.myblbl.core.common.log.HomeVideoCardDebugLogger
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.repository.UserRepository
import com.tutu.myblbl.repository.VideoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HotViewModel(
    private val videoRepository: VideoRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoModel>>(emptyList())
    val videos: StateFlow<List<VideoModel>> = _videos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String?>()
    val error: SharedFlow<String?> = _error.asSharedFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    fun loadHotList(page: Int, pageSize: Int) {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                videoRepository.getHotList(page, pageSize)
            }.onSuccess { response ->
                _loading.value = false
                if (response.isSuccess) {
                    val items = response.data ?: emptyList()
                    HomeVideoCardDebugLogger.logRejectedCards(
                        source = "hot(page=$page,pageSize=$pageSize)",
                        items = items
                    )
                    val validItems = items.filter { it.isSupportedHomeVideoCard }
                    _videos.value = validItems
                    _hasMore.value = validItems.size >= pageSize
                    enrichFollowStatus(validItems)
                } else {
                    _error.emit(response.message)
                }
            }.onFailure { throwable ->
                _loading.value = false
                _error.emit(throwable.message)
            }
        }
    }

    private fun enrichFollowStatus(videos: List<VideoModel>) {
        val mids = videos.mapNotNull { it.owner?.mid }.distinct()
        if (mids.isEmpty()) return
        viewModelScope.launch {
            val followedMids = userRepository.batchCheckFollowed(mids)
            if (followedMids.isEmpty()) return@launch
            val updated = _videos.value.map { video ->
                val mid = video.owner?.mid ?: 0L
                if (mid in followedMids && !video.isFollowed) video.copy(isFollowed = true) else video
            }
            _videos.value = updated
        }
    }

}
