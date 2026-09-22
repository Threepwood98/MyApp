package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.TrackingCheckpointKind
import com.couchlist.app.core.domain.model.TrackingCheckpointOrigin
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingSession
import com.couchlist.app.core.domain.model.TrackingSummary
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    fun observeCurrent(libraryItemId: Long): Flow<TrackingSession?>

    fun observeCurrentByMediaId(mediaId: Long): Flow<TrackingSession?>

    fun observeEnjoying(): Flow<List<LibraryMedia>>

    suspend fun start(
        mediaId: Long,
        mode: TrackingMode,
        counterTotal: Double? = null,
        counterUnit: String? = null,
    ): Long

    suspend fun pause(libraryItemId: Long)

    suspend fun resume(libraryItemId: Long)

    suspend fun complete(libraryItemId: Long)

    suspend fun abandon(libraryItemId: Long)

    suspend fun updateCounter(
        libraryItemId: Long,
        current: Double,
        total: Double?,
        unit: String?,
    )

    suspend fun addCheckpoint(
        libraryItemId: Long,
        label: String,
        parentId: Long? = null,
        kind: TrackingCheckpointKind = TrackingCheckpointKind.ITEM,
        origin: TrackingCheckpointOrigin = TrackingCheckpointOrigin.USER,
        stableKey: String? = null,
    ): Long

    suspend fun setCheckpointCompleted(checkpointId: Long, completed: Boolean)

    suspend fun addQuickLog(
        libraryItemId: Long,
        note: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ): Long

    suspend fun addJournalEntry(
        libraryItemId: Long,
        title: String,
        notes: String? = null,
        imageUri: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ): Long

    suspend fun restoreCurrent(libraryItemId: Long, summary: TrackingSummary?)
}
