package com.tutu.myblbl.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutu.myblbl.model.live.LiveRoomItem
import com.tutu.myblbl.repository.LiveRoomPage
import com.tutu.myblbl.repository.LiveRepository
import com.tutu.myblbl.core.common.log.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveListViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {

    enum class LiveListStatus {
        Idle,
        Content,
        Empty,
        Error
    }

    private val _rooms = MutableStateFlow<List<LiveRoomItem>>(emptyList())
    val rooms: StateFlow<List<LiveRoomItem>> = _rooms.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _status = MutableStateFlow(LiveListStatus.Idle)
    val status: StateFlow<LiveListStatus> = _status.asStateFlow()

    private var currentAreaId: Long = 0
    private var currentParentAreaId: Long = 0
    private var currentPage = 0

    fun startArea(parentAreaId: Long, areaId: Long, preserveExisting: Boolean = false) {
        val areaChanged = currentParentAreaId != parentAreaId || currentAreaId != areaId
        currentParentAreaId = parentAreaId
        currentAreaId = areaId
        currentPage = 0
        _hasMore.value = true
        if (areaChanged || !preserveExisting) {
            _rooms.value = emptyList()
            _status.value = LiveListStatus.Idle
        }
        loadNextPage()
    }

    fun loadNextPage() {
        if (_loading.value || !_hasMore.value) {
            return
        }

        val nextPage = currentPage + 1

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            if (currentPage == 0 && _rooms.value.isEmpty()) {
                _status.value = LiveListStatus.Idle
            }

            liveRepository.getAreaLive(currentParentAreaId, currentAreaId, nextPage).fold(
                onSuccess = { roomPage ->
                    applyRoomPage(nextPage = nextPage, roomPage = roomPage)
                },
                onFailure = ::handleLoadFailure
            )

            _loading.value = false
        }
    }

    private fun applyRoomPage(nextPage: Int, roomPage: LiveRoomPage) {
        _rooms.value = if (nextPage == 1) {
            roomPage.rooms
        } else {
            _rooms.value + roomPage.rooms
        }
        _hasMore.value = roomPage.hasMore
        currentPage = nextPage
        _status.value = if (_rooms.value.isEmpty()) {
            LiveListStatus.Empty
        } else {
            LiveListStatus.Content
        }
    }

    private fun handleLoadFailure(exception: Throwable) {
        AppLog.e("LiveListVM", "handleLoadFailure: ${exception.message}")
        _error.value = exception.message
        _status.value = if (_rooms.value.isEmpty()) {
            LiveListStatus.Error
        } else {
            LiveListStatus.Content
        }
    }
}
