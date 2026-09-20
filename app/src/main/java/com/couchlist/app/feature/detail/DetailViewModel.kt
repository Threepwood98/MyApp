package com.couchlist.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.common.networkErrorMessage
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.CatalogRepository
import com.couchlist.app.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
    private val catalogRepository: CatalogRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<Long>("mediaId") ?: -1L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    private val providers = MutableStateFlow<List<WatchProvider>>(emptyList())

    init {
        if (mediaId < 0L) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Couldn't load this title.")
            }
        } else {
            observeDetail()
            refresh()
        }
    }

    fun onRetry() {
        refresh()
    }

    fun onAddToWatchlist() {
        viewModelScope.launch {
            val state = _uiState.value
            val detail = state.detail ?: return@launch
            if (state.status != null) {
                _events.send(DetailEvent.ShowMessage("${detail.title} is already in your Watchlist"))
                return@launch
            }
            libraryRepository.addToWatchlist(mediaId)
            _events.send(DetailEvent.ShowMessage("Added ${detail.title} to your Watchlist"))
        }
    }

    fun onSetStatus(status: MediaStatus) {
        viewModelScope.launch {
            val state = _uiState.value
            state.detail ?: return@launch
            val entryId = state.entryId ?: libraryRepository.addToWatchlist(mediaId)
            libraryRepository.moveItem(entryId, status)
            _events.send(DetailEvent.ShowMessage("Moved to ${status.displayName}"))
        }
    }

    fun onRemoveFromWatchlist() {
        viewModelScope.launch {
            val entryId = _uiState.value.entryId
            val title = _uiState.value.detail?.title ?: "title"
            if (entryId != null) {
                libraryRepository.removeItem(entryId)
            }
            _events.send(DetailEvent.ShowMessage("Removed $title from your lists"))
        }
    }

    private fun observeDetail() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                catalogRepository.observeMedia(mediaId),
                libraryRepository.observeEntry(mediaId),
                providers,
            ) { media, entry, providers -> Triple(media, entry, providers) }
                .collect { (media, entry, currentProviders) ->
                    _uiState.update { state ->
                        state.copy(
                            detail = media?.toDetail(currentProviders),
                            entryId = entry?.id,
                            status = entry?.status,
                            isLoading = media == null && state.isLoading,
                            errorMessage = if (media != null) null else state.errorMessage,
                        )
                    }
                }
        }
    }

    private fun refresh() {
        if (mediaId < 0L) return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = state.detail == null,
                    errorMessage = null,
                    isOffline = false,
                )
            }
            val result = try {
                catalogRepository.refresh(mediaId)
            } catch (e: CancellationException) {
                throw e
            }
            result.onSuccess { refreshedProviders ->
                providers.value = refreshedProviders
                _uiState.update { it.copy(isLoading = false, isOffline = false) }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isOffline = state.detail != null,
                        errorMessage = if (state.detail == null) {
                            networkErrorMessage(throwable)
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

data class DetailUiState(
    val detail: MediaDetail? = null,
    val entryId: Long? = null,
    val status: MediaStatus? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
)

private fun MediaItem.toDetail(providers: List<WatchProvider>) = MediaDetail(
    id = tmdbId,
    mediaType = mediaType,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    releaseDate = releaseDate,
    originalLanguage = originalLanguage,
    runtimeMinutes = runtimeMinutes,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = externalRating,
    voteCount = externalVoteCount,
    genres = genres,
    providers = providers,
)

sealed interface DetailEvent {
    data class ShowMessage(val message: String) : DetailEvent
}
