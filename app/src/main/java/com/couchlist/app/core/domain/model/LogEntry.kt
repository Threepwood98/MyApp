package com.couchlist.app.core.domain.model

import kotlinx.serialization.Serializable

/**
 * What a logbook entry represents. The logbook is append-only: re-watching or
 * re-reviewing creates a new entry rather than overwriting a previous one.
 */
@Serializable
enum class LogAction {
    MOVIE_WATCHED,
    EPISODE_WATCHED,
    EPISODE_UNWATCHED,
    COMPLETED,
}

data class LogEntry(
    val id: Long,
    val mediaItemId: Long,
    val episodeId: Long?,
    val action: LogAction,
    val date: Long,
    val personalRating: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(personalRating == null || personalRating in 1..10)
    }
}

/** A log entry together with basic media info for display in the logbook. */
data class LogMediaEntry(
    val entry: LogEntry,
    val mediaTitle: String,
    val artworkUri: String?,
    val category: MediaCategory,
)
