package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.entity.LibraryItemEntity

internal suspend fun LibraryItemDao.getOrCreate(mediaId: Long, now: Long): Long {
    getByMediaId(mediaId)?.let { return it.id }
    return insertIgnore(
        LibraryItemEntity(
            mediaId = mediaId,
            personalRating = null,
            favorite = false,
            notes = null,
            addedAt = now,
            updatedAt = now,
        ),
    ).takeIf { it != -1L } ?: checkNotNull(getByMediaId(mediaId)).id
}
