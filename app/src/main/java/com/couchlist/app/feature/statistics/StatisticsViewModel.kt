package com.couchlist.app.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.Statistics
import com.couchlist.app.core.domain.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    statisticsRepository: StatisticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            statisticsRepository.observeStatistics().collect { stats ->
                _uiState.value = StatisticsUiState(
                    stats = stats,
                    isLoading = false,
                )
            }
        }
    }
}

data class StatisticsUiState(
    val stats: Statistics? = null,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean
        get() = stats?.let {
            it.moviesWatched == 0 && it.episodesWatched == 0
        } ?: true
}
