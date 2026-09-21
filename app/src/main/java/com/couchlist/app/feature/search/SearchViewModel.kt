package com.couchlist.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.common.networkErrorMessage
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.repository.CatalogRepository
import com.couchlist.app.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
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
@OptIn(FlowPreview::class)
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()

    private val libraryMediaReferences = MutableStateFlow<Set<MediaReference>>(emptySet())

    init {
        viewModelScope.launch {
            libraryRepository.observeAllMediaReferences().collect { references ->
                libraryMediaReferences.value = references
                refreshInLibraryStatus()
            }
        }
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
            try {
                val media = catalogRepository.getOrCreate(result)
                if (libraryRepository.isInLibrary(media.id)) {
                    _events.send(
                        SearchEvent.ShowMessage("${result.title} is already in your Watchlist"),
                    )
                } else {
                    libraryRepository.addToWatchlist(media.id)
                    _events.send(
                        SearchEvent.ShowMessage("Added ${result.title} to your Watchlist"),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(SearchEvent.ShowMessage("Couldn't add ${result.title}"))
            }
        }
    }

    fun onResultClick(result: MediaSearchResult) {
        viewModelScope.launch {
            try {
                val media = catalogRepository.getOrCreate(result)
                _events.send(SearchEvent.NavigateToDetail(media.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(SearchEvent.ShowMessage("Couldn't open ${result.title}"))
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
        catalogRepository.search(query.trim())
            .onSuccess { results ->
                val references = results.map { it.reference }.toSet()
                val foundInLibrary = libraryMediaReferences.value.intersect(references)
                _uiState.update {
                    it.copy(
                        results = results,
                        inLibraryReferences = foundInLibrary,
                        isSearching = false,
                        errorMessage = null,
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(isSearching = false, errorMessage = networkErrorMessage(throwable))
                }
            }
    }

    private fun refreshInLibraryStatus() {
        val currentReferences = libraryMediaReferences.value
        _uiState.update { state ->
            state.copy(
                inLibraryReferences = state.results
                    .map { it.reference }
                    .toSet()
                    .intersect(currentReferences),
            )
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: List<MediaSearchResult> = emptyList(),
    val inLibraryReferences: Set<MediaReference> = emptySet(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface SearchEvent {
    data class ShowMessage(val message: String) : SearchEvent

    data class NavigateToDetail(val mediaId: Long) : SearchEvent
}
