package com.couchlist.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.nextStatus
import com.couchlist.app.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeStatus(MediaStatus.BACKLOG),
                repository.observeStatus(MediaStatus.WATCHING),
                repository.observeStatus(MediaStatus.COMPLETED),
                repository.observeStatus(MediaStatus.ABANDONED),
            ) { backlog, watching, completed, abandoned ->
                HomeUiState(
                    backlog = backlog,
                    watching = watching,
                    completed = completed,
                    abandoned = abandoned,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onAdvanceStatus(item: LibraryMedia) {
        val next = item.library.status.nextStatus ?: return
        viewModelScope.launch { repository.moveItem(item.library.id, next) }
    }

    fun onRemove(item: LibraryMedia) {
        viewModelScope.launch {
            repository.removeItem(item.library.id)
            _events.send(HomeEvent.ShowMessage("Removed ${item.media.title}"))
        }
    }
}

data class HomeUiState(
    val backlog: List<LibraryMedia> = emptyList(),
    val watching: List<LibraryMedia> = emptyList(),
    val completed: List<LibraryMedia> = emptyList(),
    val abandoned: List<LibraryMedia> = emptyList(),
)

sealed interface HomeEvent {
    data class ShowMessage(val message: String) : HomeEvent
}
