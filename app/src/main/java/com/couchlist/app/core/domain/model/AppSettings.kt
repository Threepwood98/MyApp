package com.couchlist.app.core.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val providerRegion: String = "US",
)

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}
