package com.couchlist.app.core.domain.model

data class MediaItem(
    val id: Long,
    val mediaType: MediaType,
    val tmdbId: Long,
    val title: String,
    val posterPath: String?,
    val status: WatchStatus,
    val sortOrder: Int,
    val addedAt: Long,
)