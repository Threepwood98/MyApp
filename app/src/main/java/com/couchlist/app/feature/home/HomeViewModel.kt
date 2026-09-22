package com.couchlist.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLibrary().collect { library ->
                _uiState.value = HomeUiState(
                    continueWatching = library
                        .filter { it.tracking?.state == TrackingState.ACTIVE }
                        .sortedByDescending { it.tracking?.updatedAt }
                        .take(DASHBOARD_LIMIT),
                    recentlyAdded = library
                        .sortedByDescending { it.library.addedAt }
                        .take(DASHBOARD_LIMIT),
                    recentlyCompleted = library
                        .filter { it.tracking?.state == TrackingState.COMPLETED }
                        .sortedByDescending {
                            it.tracking?.endedAt ?: it.tracking?.updatedAt
                        }
                        .take(DASHBOARD_LIMIT),
                )
            }
        }
    }

    private companion object {
        const val DASHBOARD_LIMIT = 10
    }
}

data class HomeUiState(
    val continueWatching: List<LibraryMedia> = emptyList(),
    val recentlyAdded: List<LibraryMedia> = emptyList(),
    val recentlyCompleted: List<LibraryMedia> = emptyList(),
) {
    val isEmpty: Boolean
        get() = continueWatching.isEmpty() && recentlyAdded.isEmpty() && recentlyCompleted.isEmpty()
}
