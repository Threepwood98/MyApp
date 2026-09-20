package com.couchlist.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.common.networkErrorMessage
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.WatchStatus
import com.couchlist.app.core.domain.repository.MediaRepository
import com.couchlist.app.core.domain.repository.WatchlistRepository
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
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    private val tmdbId: Long = savedStateHandle.get<Long>("tmdbId") ?: -1L
    private val mediaType: MediaType? = savedStateHandle
        .get<String>("mediaType")
        ?.let { runCatching { MediaType.valueOf(it) }.getOrNull() }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    init {
        if (mediaType == null || tmdbId < 0L) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Couldn't load this title.")
            }
        } else {
            viewModelScope.launch { load() }
        }
    }

    fun onRetry() {
        if (_uiState.value.detail == null && mediaType != null && tmdbId >= 0L) {
            viewModelScope.launch { load() }
        }
    }

    fun onAddToWatchlist() {
        viewModelScope.launch {
            val state = _uiState.value
            val detail = state.detail ?: return@launch
            if (state.status != null) {
                _events.send(DetailEvent.ShowMessage("${detail.title} is already in your Watchlist"))
                return@launch
            }
            watchlistRepository.addToWatchlist(
                mediaType = detail.mediaType,
                tmdbId = detail.id,
                title = detail.title,
                posterPath = detail.posterPath,
            )
            _events.send(DetailEvent.ShowMessage("Added ${detail.title} to your Watchlist"))
        }
    }

    fun onSetStatus(status: WatchStatus) {
        viewModelScope.launch {
            val state = _uiState.value
            val detail = state.detail ?: return@launch
            val entryId = state.entryId ?: watchlistRepository.addToWatchlist(
                mediaType = detail.mediaType,
                tmdbId = detail.id,
                title = detail.title,
                posterPath = detail.posterPath,
            )
            watchlistRepository.moveItem(entryId, status)
            _events.send(DetailEvent.ShowMessage("Moved to ${status.displayName}"))
        }
    }

    fun onRemoveFromWatchlist() {
        viewModelScope.launch {
            val entryId = _uiState.value.entryId
            val title = _uiState.value.detail?.title ?: "title"
            if (entryId != null) {
                watchlistRepository.removeItem(entryId)
            }
            _events.send(DetailEvent.ShowMessage("Removed $title from your lists"))
        }
    }

    private suspend fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val enabledType = mediaType ?: return
        val detail = mediaRepository.details(tmdbId, enabledType)
            .getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = networkErrorMessage(error))
                }
                return
            }
        watchlistRepository.observeEntry(tmdbId, enabledType).collect { entry ->
            _uiState.update {
                it.copy(
                    detail = detail,
                    entryId = entry?.id,
                    status = entry?.status,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }
}

data class DetailUiState(
    val detail: MediaDetail? = null,
    val entryId: Long? = null,
    val status: WatchStatus? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface DetailEvent {
    data class ShowMessage(val message: String) : DetailEvent
}