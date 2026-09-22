package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingState

/**
 * A library item joined with its catalog metadata, as returned by the library
 * status queries. Media columns are aliased with a `m_` prefix in the query.
 */
data class LibraryMediaRow(
    @Embedded(prefix = "m_")
    val media: MediaItemEntity,
    @ColumnInfo(name = "m_runtime_minutes")
    val runtimeMinutes: Int?,
    @ColumnInfo(name = "lib_id")
    val libraryId: Long,
    @ColumnInfo(name = "lib_personal_rating")
    val personalRating: Int?,
    @ColumnInfo(name = "lib_favorite")
    val favorite: Boolean,
    @ColumnInfo(name = "lib_notes")
    val notes: String?,
    @ColumnInfo(name = "lib_added_at")
    val addedAt: Long,
    @ColumnInfo(name = "lib_updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "tracking_session_id")
    val trackingSessionId: Long?,
    @ColumnInfo(name = "tracking_mode")
    val trackingMode: TrackingMode?,
    @ColumnInfo(name = "tracking_state")
    val trackingState: TrackingState?,
    @ColumnInfo(name = "tracking_started_at")
    val trackingStartedAt: Long?,
    @ColumnInfo(name = "tracking_ended_at")
    val trackingEndedAt: Long?,
    @ColumnInfo(name = "tracking_updated_at")
    val trackingUpdatedAt: Long?,
    @ColumnInfo(name = "tracking_legacy_progress")
    val trackingLegacyProgress: Double?,
    @ColumnInfo(name = "tracking_counter_current")
    val trackingCounterCurrent: Double?,
    @ColumnInfo(name = "tracking_counter_total")
    val trackingCounterTotal: Double?,
    @ColumnInfo(name = "tracking_checkpoint_total")
    val trackingCheckpointTotal: Int?,
    @ColumnInfo(name = "tracking_checkpoint_completed")
    val trackingCheckpointCompleted: Int?,
)
