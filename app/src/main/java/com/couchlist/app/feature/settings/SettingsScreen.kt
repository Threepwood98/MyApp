package com.couchlist.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.couchlist.app.BuildConfig
import com.couchlist.app.core.domain.model.AppSettings
import com.couchlist.app.core.domain.model.ThemeMode
import com.couchlist.app.core.ui.theme.CouchlistTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        settings = uiState,
        onBack = onBack,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        onProviderRegionChange = viewModel::onProviderRegionChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onProviderRegionChange: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            SectionHeader(text = "Appearance")
            ListItem(
                headlineContent = { Text(text = "Theme") },
                supportingContent = {
                    Text(text = "Choose how Couchlist looks on this device.")
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(text = mode.displayName) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text(text = "Dynamic color") },
                supportingContent = {
                    Text(text = "Use colors inspired by your wallpaper on Android 12+.")
                },
                trailingContent = {
                    Switch(
                        checked = settings.dynamicColorEnabled,
                        onCheckedChange = onDynamicColorChange,
                    )
                },
            )
            HorizontalDivider()
            SectionHeader(text = "Streaming")
            ListItem(
                headlineContent = { Text(text = "Provider region") },
                supportingContent = {
                    Text(text = "Controls which streaming services appear on details.")
                },
                trailingContent = {
                    RegionMenu(
                        selectedRegion = settings.providerRegion,
                        onRegionSelected = onProviderRegionChange,
                    )
                },
            )
            HorizontalDivider()
            SectionHeader(text = "About")
            ListItem(
                headlineContent = { Text(text = "Version") },
                supportingContent = {
                    Text(text = "Your watchlist, wherever you sit.")
                },
                trailingContent = {
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Movies and TV data by The Movie Database (TMDB)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RegionMenu(
    selectedRegion: String,
    onRegionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = regionOptions.firstOrNull { it.code == selectedRegion }?.label ?: selectedRegion)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            regionOptions.forEach { region ->
                DropdownMenuItem(
                    text = { Text(text = region.label) },
                    onClick = {
                        expanded = false
                        onRegionSelected(region.code)
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { heading() },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    CouchlistTheme {
        SettingsContent(
            settings = AppSettings(),
            onBack = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onProviderRegionChange = {},
        )
    }
}

private data class RegionOption(val code: String, val label: String)

private val regionOptions = listOf(
    RegionOption("US", "United States"),
    RegionOption("GB", "United Kingdom"),
    RegionOption("CA", "Canada"),
    RegionOption("AU", "Australia"),
    RegionOption("DE", "Germany"),
    RegionOption("FR", "France"),
    RegionOption("ES", "Spain"),
    RegionOption("IT", "Italy"),
    RegionOption("JP", "Japan"),
)
