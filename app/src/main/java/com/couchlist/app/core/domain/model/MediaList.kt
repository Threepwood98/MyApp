package com.couchlist.app.core.domain.model

enum class MediaListType {
    PILE,
    TODO,
    COLLECTION,
}

/**
 * A named collection a media item can belong to. Membership is independent from
 * [MediaStatus]; a media item may be in any number of lists.
 */
data class MediaList(
    val id: Long,
    val name: String,
    val description: String?,
    val type: MediaListType,
    val coverMediaId: Long?,
    val isPinned: Boolean,
    val sortOrder: Int,
    val smartFilterJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
