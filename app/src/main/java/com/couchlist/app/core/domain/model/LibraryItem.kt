package com.couchlist.app.core.domain.model

/**
 * User-specific state for one media item. Exists at most once per [MediaItem];
 * list membership is tracked separately via [MediaList], never via status.
 */
data class LibraryItem(
    val id: Long,
    val mediaId: Long,
    val status: MediaStatus,
    val progress: Double?,
    val personalRating: Int?,
    val favorite: Boolean,
    val notes: String?,
    val addedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val updatedAt: Long,
) {
    init {
        require(personalRating == null || personalRating in 1..10)
    }
}

data class ListMembership(
    val listId: Long,
    val addedAt: Long,
)

data class LibraryRemoval(
    val item: LibraryItem,
    val memberships: List<ListMembership>,
)
