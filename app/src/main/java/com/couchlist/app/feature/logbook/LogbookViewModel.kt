package com.couchlist.app.feature.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.LogMediaEntry
import com.couchlist.app.core.domain.repository.LogbookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LogbookViewModel @Inject constructor(
    private val logbookRepository: LogbookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogbookUiState())
    val uiState: StateFlow<LogbookUiState> = _uiState.asStateFlow()

    init {
        observeEntries()
    }

    fun onFilterChanged(filter: LogAction?) {
        _uiState.update { it.copy(activeFilter = filter) }
        observeEntries()
    }

    private fun observeEntries() {
        val filter = _uiState.value.activeFilter
        val flow = if (filter != null) {
            logbookRepository.observeByAction(filter)
        } else {
            logbookRepository.observeAll()
        }
        viewModelScope.launch {
            flow.collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
    }
}

data class LogbookUiState(
    val entries: List<LogMediaEntry> = emptyList(),
    val activeFilter: LogAction? = null,
)
