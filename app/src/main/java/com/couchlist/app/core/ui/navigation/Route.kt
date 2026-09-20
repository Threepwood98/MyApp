package com.couchlist.app.core.ui.navigation

import com.couchlist.app.core.domain.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data class DetailRoute(
    val tmdbId: Long,
    val mediaType: MediaType,
)