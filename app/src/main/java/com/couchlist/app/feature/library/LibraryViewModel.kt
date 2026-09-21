package com.couchlist.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.LibraryRemoval
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.SmartFilter
import com.couchlist.app.core.domain.model.nextStatus
import com.couchlist.app.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeLibrary(),
                repository.observeLists(),
            ) { items: List<LibraryMedia>, lists: List<MediaListSummary> ->
                _uiState.value.copy(items = items, lists = lists)
            }.collect { _uiState.value = it }
        }
    }

    fun onSortOptionSelected(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun onMultiSelectModeEnter() {
        _uiState.update { it.copy(isMultiSelectMode = true, selectedIds = emptySet()) }
    }

    fun onMultiSelectModeExit() {
        _uiState.update { it.copy(isMultiSelectMode = false, selectedIds = emptySet()) }
    }

    fun onItemToggleSelection(mediaId: Long) {
        _uiState.update { state ->
            val newIds = if (mediaId in state.selectedIds) {
                state.selectedIds - mediaId
            } else {
                state.selectedIds + mediaId
            }
            state.copy(selectedIds = newIds)
        }
    }

    fun onSelectAll() {
        val status = _uiState.value.currentStatusTab
        val items = getFilteredSortedItems().filter { it.library.status == status }
        _uiState.update { it.copy(selectedIds = items.map { i -> i.media.id }.toSet()) }
    }

    fun onBatchMoveStatus() {
        viewModelScope.launch {
            val state = _uiState.value
            val targetStatus = state.currentStatusTab.nextStatus ?: return@launch
            val count = state.selectedIds.size
            state.selectedIds.forEach { mediaId ->
                val item = state.items.firstOrNull { it.media.id == mediaId } ?: return@forEach
                repository.moveItem(item.library.id, targetStatus)
            }
            _uiState.update { it.copy(isMultiSelectMode = false, selectedIds = emptySet()) }
            _events.send(
                LibraryEvent.ShowUndo(
                    message = "Moved $count titles to ${targetStatus.displayName}",
                    action = LibraryUndo.BatchStatus(state.items.filter { it.media.id in state.selectedIds }.map { it.library }),
                ),
            )
        }
    }

    fun onBatchRemove() {
        viewModelScope.launch {
            val state = _uiState.value
            val count = state.selectedIds.size
            val removals = mutableListOf<LibraryRemoval>()
            state.selectedIds.forEach { mediaId ->
                val item = state.items.firstOrNull { it.media.id == mediaId } ?: return@forEach
                repository.removeItem(item.library.id)?.let { removals.add(it) }
            }
            _uiState.update { it.copy(isMultiSelectMode = false, selectedIds = emptySet()) }
            _events.send(
                LibraryEvent.ShowUndo(
                    message = "Removed $count titles",
                    action = LibraryUndo.BatchRemoval(removals),
                ),
            )
        }
    }

    fun onAdvanceStatus(item: LibraryMedia) {
        val next = item.library.status.nextStatus ?: return
        viewModelScope.launch {
            repository.moveItem(item.library.id, next)
            _events.send(
                LibraryEvent.ShowUndo(
                    message = "Moved ${item.media.title} to ${next.displayName}",
                    action = LibraryUndo.Status(item.library),
                ),
            )
        }
    }

    fun onRemove(item: LibraryMedia) {
        viewModelScope.launch {
            val removal = repository.removeItem(item.library.id) ?: return@launch
            _events.send(
                LibraryEvent.ShowUndo(
                    message = "Removed ${item.media.title}",
                    action = LibraryUndo.Removal(removal),
                ),
            )
        }
    }

    fun onUndo(action: LibraryUndo) {
        viewModelScope.launch {
            when (action) {
                is LibraryUndo.Status -> repository.restoreItemState(action.previous)
                is LibraryUndo.Removal -> repository.restoreRemoval(action.removal)
                is LibraryUndo.BatchStatus -> action.previous.forEach { repository.restoreItemState(it) }
                is LibraryUndo.BatchRemoval -> action.removals.forEach { repository.restoreRemoval(it) }
            }
        }
    }

    fun onCreateList(name: String, description: String?, type: MediaListType) {
        viewModelScope.launch {
            repository.createList(name, description, type)
            _events.send(LibraryEvent.ShowMessage("Created list \"$name\""))
        }
    }

    fun onCreateSmartList(name: String, description: String?, filter: SmartFilter) {
        viewModelScope.launch {
            val json = SmartFilter.toJson(filter)
            repository.createList(name, description, MediaListType.SMART_LIST, json)
            _events.send(LibraryEvent.ShowMessage("Created smart list \"$name\""))
        }
    }

    fun onSmartListClick(listId: Long) {
        viewModelScope.launch {
            val json = repository.getSmartFilterJson(listId)
            val filter = SmartFilter.fromJson(json) ?: return@launch
            _uiState.update { it.copy(selectedSmartListId = listId, selectedSmartListFilter = filter) }
        }
    }

    fun onSmartListBack() {
        _uiState.update { it.copy(selectedSmartListId = null, selectedSmartListFilter = null) }
    }

    fun onDeleteList(listId: Long) {
        viewModelScope.launch {
            repository.deleteList(listId)
            if (_uiState.value.selectedSmartListId == listId) {
                _uiState.update { it.copy(selectedSmartListId = null, selectedSmartListFilter = null) }
            }
            _events.send(LibraryEvent.ShowMessage("List deleted"))
        }
    }

    fun onStatusTabChanged(status: MediaStatus) {
        _uiState.update { it.copy(currentStatusTab = status) }
    }

    fun getFilteredSortedItems(): List<LibraryMedia> {
        val state = _uiState.value
        val items = state.items
        return when (state.sortOption) {
            SortOption.DATE_ADDED -> items.sortedByDescending { it.library.addedAt }
            SortOption.TITLE -> items.sortedBy { it.media.title.lowercase() }
            SortOption.RATING -> items.sortedByDescending { it.library.personalRating ?: 0 }
        }
    }
}

data class LibraryUiState(
    val items: List<LibraryMedia> = emptyList(),
    val lists: List<MediaListSummary> = emptyList(),
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val currentStatusTab: MediaStatus = MediaStatus.BACKLOG,
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val selectedSmartListId: Long? = null,
    val selectedSmartListFilter: SmartFilter? = null,
)

enum class SortOption(val displayName: String) {
    DATE_ADDED("Date added"),
    TITLE("Title"),
    RATING("Rating"),
}

sealed interface LibraryUndo {
    data class Status(val previous: LibraryItem) : LibraryUndo
    data class Removal(val removal: LibraryRemoval) : LibraryUndo
    data class BatchStatus(val previous: List<LibraryItem>) : LibraryUndo
    data class BatchRemoval(val removals: List<LibraryRemoval>) : LibraryUndo
}

sealed interface LibraryEvent {
    data class ShowUndo(
        val message: String,
        val action: LibraryUndo,
    ) : LibraryEvent

    data class ShowMessage(val message: String) : LibraryEvent
}
