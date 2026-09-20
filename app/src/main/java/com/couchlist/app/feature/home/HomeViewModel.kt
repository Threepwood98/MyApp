package com.couchlist.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.WatchStatus
import com.couchlist.app.core.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: WatchlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeStatus(WatchStatus.WATCHLIST),
                repository.observeStatus(WatchStatus.WATCHING),
                repository.observeStatus(WatchStatus.WATCHED),
            ) { watchlist, watching, watched ->
                HomeUiState(
                    watchlist = watchlist,
                    watching = watching,
                    watched = watched,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}

data class HomeUiState(
    val watchlist: List<MediaItem> = emptyList(),
    val watching: List<MediaItem> = emptyList(),
    val watched: List<MediaItem> = emptyList(),
)