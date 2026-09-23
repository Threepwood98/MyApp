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
    private val pileMediaReferences = MutableStateFlow<Set<MediaReference>>(emptySet())

    init {
        viewModelScope.launch {
            libraryRepository.observeAllMediaReferences().collect { references ->
                libraryMediaReferences.value = references
                refreshMembershipStatus()
            }
        }
        viewModelScope.launch {
            libraryRepository.observePileMediaReferences().collect { references ->
                pileMediaReferences.value = references
                refreshMembershipStatus()
            }
        }
        viewModelScope.launch {
            _uiState
                .map { it.query }
                .distinctUntilChanged()
                .debounce(400)
                .collectLatest { query -> search(query) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                isSearching = query.isNotBlank(),
                errorMessage = null,
            )
        }
    }

    fun onRetry() {
        viewModelScope.launch { search(uiState.value.query) }
    }

    fun onAddToPile(result: MediaSearchResult) {
        viewModelScope.launch {
            try {
                val media = catalogRepository.getOrCreate(result)
                if (result.reference in pileMediaReferences.value) {
                    _events.send(
                        SearchEvent.ShowMessage("${result.title} is already in The Pile"),
                    )
                } else {
                    libraryRepository.addToPile(media.id)
                    _events.send(
                        SearchEvent.ShowMessage("Added ${result.title} to The Pile"),
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
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            _uiState.update { state ->
                if (state.query.isBlank()) {
                    state.copy(results = emptyList(), isSearching = false, errorMessage = null)
                } else {
                    state
                }
            }
            return
        }
        _uiState.update { state ->
            if (state.query.trim() == normalizedQuery) {
                state.copy(isSearching = true, errorMessage = null)
            } else {
                state
            }
        }
        catalogRepository.search(normalizedQuery)
            .onSuccess { results ->
                val references = results.map { it.reference }.toSet()
                val foundInLibrary = libraryMediaReferences.value.intersect(references)
                val foundInPile = pileMediaReferences.value.intersect(references)
                _uiState.update { state ->
                    if (state.query.trim() == normalizedQuery) {
                        state.copy(
                            results = results,
                            inLibraryReferences = foundInLibrary,
                            inPileReferences = foundInPile,
                            isSearching = false,
                            errorMessage = null,
                        )
                    } else {
                        state
                    }
                }
            }
            .onFailure { throwable ->
                _uiState.update { state ->
                    if (state.query.trim() == normalizedQuery) {
                        state.copy(
                            isSearching = false,
                            errorMessage = networkErrorMessage(throwable),
                        )
                    } else {
                        state
                    }
                }
            }
    }

    private fun refreshMembershipStatus() {
        val libraryReferences = libraryMediaReferences.value
        val pileReferences = pileMediaReferences.value
        _uiState.update { state ->
            state.copy(
                inLibraryReferences = state.results
                    .map { it.reference }
                    .toSet()
                    .intersect(libraryReferences),
                inPileReferences = state.results
                    .map { it.reference }
                    .toSet()
                    .intersect(pileReferences),
            )
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: List<MediaSearchResult> = emptyList(),
    val inLibraryReferences: Set<MediaReference> = emptySet(),
    val inPileReferences: Set<MediaReference> = emptySet(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface SearchEvent {
    data class ShowMessage(val message: String) : SearchEvent

    data class NavigateToDetail(val mediaId: Long) : SearchEvent
}
