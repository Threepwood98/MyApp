package com.couchlist.app.feature.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.defaultTrackingMode
import com.couchlist.app.core.domain.model.defaultTrackingUnit
import com.couchlist.app.core.domain.repository.LibraryRepository
import com.couchlist.app.core.domain.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LibraryRepository,
    private val trackingRepository: TrackingRepository,
) : ViewModel() {

    private val listId: Long = savedStateHandle.get<Long>("listId") ?: -1L

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ListDetailEvent>(Channel.BUFFERED)
    val events: Flow<ListDetailEvent> = _events.receiveAsFlow()

    init {
        if (listId < 0L) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                repository.observeList(listId).collect { list ->
                    _uiState.update { it.copy(list = list, isLoading = false) }
                }
            }
            viewModelScope.launch {
                repository.observeListItems(listId).collect { items ->
                    _uiState.update { it.copy(items = items) }
                }
            }
            viewModelScope.launch {
                repository.observeLists().collect { lists ->
                    _uiState.update {
                        it.copy(
                            targetLists = lists.filter { summary ->
                                summary.list.id != listId &&
                                    summary.list.type != MediaListType.SMART_LIST
                            },
                        )
                    }
                }
            }
        }
    }

    fun toggleCompletedVisibility() {
        _uiState.update { it.copy(showCompleted = !it.showCompleted) }
    }

    fun remove(item: LibraryMedia) {
        viewModelScope.launch {
            repository.removeFromList(listId, item.library.id)
            _events.send(ListDetailEvent.ShowMessage("Removed ${item.media.title}"))
        }
    }

    fun copy(item: LibraryMedia, targetListId: Long) {
        viewModelScope.launch {
            repository.copyToList(targetListId, item.library.id)
            _events.send(ListDetailEvent.ShowMessage("Copied ${item.media.title}"))
        }
    }

    fun move(item: LibraryMedia, targetListId: Long) {
        viewModelScope.launch {
            repository.moveToList(listId, targetListId, item.library.id)
            _events.send(ListDetailEvent.ShowMessage("Moved ${item.media.title}"))
        }
    }

    fun startTracking(item: LibraryMedia) {
        viewModelScope.launch {
            trackingRepository.start(
                mediaId = item.media.id,
                mode = item.media.category.defaultTrackingMode,
                counterUnit = item.media.category.defaultTrackingUnit,
            )
            _events.send(ListDetailEvent.ShowMessage("Started tracking ${item.media.title}"))
        }
    }

    fun pauseTracking(item: LibraryMedia) {
        viewModelScope.launch { trackingRepository.pause(item.library.id) }
    }

    fun resumeTracking(item: LibraryMedia) {
        viewModelScope.launch { trackingRepository.resume(item.library.id) }
    }

    fun completeTracking(item: LibraryMedia) {
        viewModelScope.launch { trackingRepository.complete(item.library.id) }
    }

    fun abandonTracking(item: LibraryMedia) {
        viewModelScope.launch { trackingRepository.abandon(item.library.id) }
    }
}

data class ListDetailUiState(
    val list: MediaList? = null,
    val items: List<LibraryMedia> = emptyList(),
    val targetLists: List<MediaListSummary> = emptyList(),
    val showCompleted: Boolean = false,
    val isLoading: Boolean = true,
) {
    val visibleItems: List<LibraryMedia>
        get() = if (list?.type == MediaListType.TODO && !showCompleted) {
            items.filterNot { it.status == MediaStatus.COMPLETED }
        } else {
            items
        }
}

sealed interface ListDetailEvent {
    data class ShowMessage(val message: String) : ListDetailEvent
}
