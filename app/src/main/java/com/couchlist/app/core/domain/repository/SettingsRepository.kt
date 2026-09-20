package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.AppSettings
import com.couchlist.app.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setProviderRegion(region: String)
}
