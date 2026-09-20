package com.couchlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.couchlist.app.core.domain.model.AppSettings
import com.couchlist.app.core.domain.repository.SettingsRepository
import com.couchlist.app.core.ui.navigation.CouchlistNavHost
import com.couchlist.app.core.ui.theme.CouchlistTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            CouchlistTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColorEnabled,
            ) {
                CouchlistApp()
            }
        }
    }
}

@Composable
fun CouchlistApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    CouchlistNavHost(
        navController = navController,
        modifier = modifier.fillMaxSize(),
    )
}
