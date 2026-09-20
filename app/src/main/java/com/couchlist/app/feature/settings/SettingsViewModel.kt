package com.couchlist.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.AppSettings
import com.couchlist.app.core.domain.model.ThemeMode
import com.couchlist.app.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun onThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(themeMode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColorEnabled(enabled) }
    }

    fun onProviderRegionChange(region: String) {
        viewModelScope.launch { repository.setProviderRegion(region) }
    }
}
