package com.couchlist.app.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data object SearchRoute

@Serializable
data class DetailRoute(
    val mediaId: Long,
)
