package com.couchlist.app.core.domain.model

/** A media item together with the user's library state for it (a Home/library row). */
data class LibraryMedia(
    val media: MediaItem,
    val library: LibraryItem,
    val tracking: TrackingSummary? = null,
) {
    val status: MediaStatus
        get() = tracking?.status ?: MediaStatus.BACKLOG

    val progress: Double?
        get() = tracking?.progress
}
