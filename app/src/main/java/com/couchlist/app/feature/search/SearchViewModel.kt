package com.couchlist.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.common.networkErrorMessage
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.repository.MediaRepository
import com.couchlist.app.core.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { it.query }
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { query -> search(query) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, errorMessage = null) }
    }

    fun onRetry() {
        viewModelScope.launch { search(uiState.value.query) }
    }

    fun onAddToWatchlist(result: MediaSearchResult) {
        viewModelScope.launch {
            if (watchlistRepository.isInWatchlist(result.id, result.mediaType)) {
                _events.send(
                    SearchEvent.ShowMessage("${result.title} is already in your Watchlist"),
                )
            } else {
                watchlistRepository.addToWatchlist(
                    mediaType = result.mediaType,
                    tmdbId = result.id,
                    title = result.title,
                    posterPath = result.posterPath,
                )
                _events.send(
                    SearchEvent.ShowMessage("Added ${result.title} to your Watchlist"),
                )
            }
        }
    }

    private suspend fun search(query: String) {
        if (query.isBlank()) {
            _uiState.update {
                it.copy(results = emptyList(), isSearching = false, errorMessage = null)
            }
            return
        }
        _uiState.update { it.copy(isSearching = true, errorMessage = null) }
        mediaRepository.searchMulti(query.trim())
            .onSuccess { results ->
                _uiState.update {
                    it.copy(results = results, isSearching = false, errorMessage = null)
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(isSearching = false, errorMessage = networkErrorMessage(throwable))
                }
            }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: List<MediaSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface SearchEvent {
    data class ShowMessage(val message: String) : SearchEvent
}