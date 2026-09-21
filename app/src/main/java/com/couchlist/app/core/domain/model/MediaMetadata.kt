package com.couchlist.app.core.domain.model

sealed interface MediaMetadata {
    data class Video(
        val runtimeMinutes: Int?,
        val seasons: List<SeasonMetadata> = emptyList(),
    ) : MediaMetadata

    data object None : MediaMetadata
}
