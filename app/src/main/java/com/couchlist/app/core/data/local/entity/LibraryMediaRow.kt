package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.couchlist.app.core.domain.model.MediaStatus

/**
 * A library item joined with its catalog metadata, as returned by the library
 * status queries. Media columns are aliased with a `m_` prefix in the query.
 */
data class LibraryMediaRow(
    @Embedded(prefix = "m_")
    val media: MediaItemEntity,
    @ColumnInfo(name = "lib_id")
    val libraryId: Long,
    @ColumnInfo(name = "lib_status")
    val status: MediaStatus,
    @ColumnInfo(name = "lib_progress")
    val progress: Double?,
    @ColumnInfo(name = "lib_personal_rating")
    val personalRating: Int?,
    @ColumnInfo(name = "lib_favorite")
    val favorite: Boolean,
    @ColumnInfo(name = "lib_notes")
    val notes: String?,
    @ColumnInfo(name = "lib_added_at")
    val addedAt: Long,
    @ColumnInfo(name = "lib_started_at")
    val startedAt: Long?,
    @ColumnInfo(name = "lib_completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "lib_updated_at")
    val updatedAt: Long,
)