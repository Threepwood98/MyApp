package com.couchlist.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.couchlist.app.core.domain.model.AppSettings
import com.couchlist.app.core.domain.model.ThemeMode
import com.couchlist.app.core.domain.repository.SettingsRepository
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PreferencesSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                dynamicColorEnabled = preferences[DYNAMIC_COLOR] ?: true,
                providerRegion = preferences[PROVIDER_REGION] ?: deviceRegion(),
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = themeMode.name }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setProviderRegion(region: String) {
        dataStore.edit { it[PROVIDER_REGION] = region.uppercase(Locale.ROOT) }
    }

    private fun deviceRegion(): String = Locale.getDefault().country
        .takeIf { it.length == 2 }
        ?.uppercase(Locale.ROOT)
        ?: "US"

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PROVIDER_REGION = stringPreferencesKey("provider_region")
    }
}
