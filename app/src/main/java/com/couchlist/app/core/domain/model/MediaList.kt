package com.couchlist.app.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaListType {
    PILE,
    TODO,
    COLLECTION,
    SMART_LIST,
}

/**
 * A named collection a media item can belong to. Membership is independent from
 * [MediaStatus]; a media item may be in any number of lists.
 */
@Serializable
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

data class MediaListSummary(
    val list: MediaList,
    val itemCount: Int,
)
