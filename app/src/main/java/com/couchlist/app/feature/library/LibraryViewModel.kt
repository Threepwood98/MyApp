package com.couchlist.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.LibraryRemoval
import com.couchlist.app.core.domain.model.MediaListSummary
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
            ) { items, lists -> LibraryUiState(items = items, lists = lists) }
                .collect { _uiState.value = it }
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
            }
        }
    }
}

data class LibraryUiState(
    val items: List<LibraryMedia> = emptyList(),
    val lists: List<MediaListSummary> = emptyList(),
)

sealed interface LibraryUndo {
    data class Status(val previous: LibraryItem) : LibraryUndo

    data class Removal(val removal: LibraryRemoval) : LibraryUndo
}

sealed interface LibraryEvent {
    data class ShowUndo(
        val message: String,
        val action: LibraryUndo,
    ) : LibraryEvent
}
