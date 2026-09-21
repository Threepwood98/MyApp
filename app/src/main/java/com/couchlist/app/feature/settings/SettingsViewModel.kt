package com.couchlist.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.AppSettings
import com.couchlist.app.core.domain.model.ThemeMode
import com.couchlist.app.core.domain.repository.ExportImportRepository
import com.couchlist.app.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val exportImportRepository: ExportImportRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun onThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(themeMode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColorEnabled(enabled) }
    }

    fun onProviderRegionChange(region: String) {
        viewModelScope.launch { repository.setProviderRegion(region) }
    }

    fun onExportToFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = exportImportRepository.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { it.write(json) }
                }
                _events.emit(SettingsEvent.ShowMessage("Library exported"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowMessage("Export failed: ${e.message}"))
            }
        }
    }

    fun onImportFromFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: run {
                    _events.emit(SettingsEvent.ShowMessage("Could not read file"))
                    return@launch
                }
                exportImportRepository.importFromJson(json)
                _events.emit(SettingsEvent.ShowMessage("Library imported"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowMessage("Import failed: ${e.message}"))
            }
        }
    }
}

sealed interface SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent
}
