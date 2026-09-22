package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.TrackingDao
import com.couchlist.app.core.data.local.entity.TrackingCheckpointEntity
import com.couchlist.app.core.data.local.entity.TrackingCounterEntity
import com.couchlist.app.core.data.local.entity.TrackingJournalEntryEntity
import com.couchlist.app.core.data.local.entity.TrackingQuickLogEntity
import com.couchlist.app.core.data.local.entity.TrackingSessionEntity
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.TrackingCheckpoint
import com.couchlist.app.core.domain.model.TrackingCheckpointKind
import com.couchlist.app.core.domain.model.TrackingCheckpointOrigin
import com.couchlist.app.core.domain.model.TrackingDetails
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingSession
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.domain.model.TrackingSummary
import com.couchlist.app.core.domain.repository.TrackingRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackingRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val libraryItemDao: LibraryItemDao,
    private val trackingDao: TrackingDao,
) : TrackingRepository {
    override fun observeCurrent(libraryItemId: Long): Flow<TrackingSession?> =
        trackingDao.observeCurrent(libraryItemId).map { it?.toDomain() }

    override fun observeCurrentByMediaId(mediaId: Long): Flow<TrackingSession?> =
        trackingDao.observeCurrentByMediaId(mediaId).map { it?.toDomain() }

    override fun observeEnjoying(): Flow<List<LibraryMedia>> =
        libraryItemDao.observeByTrackingState(TrackingState.ACTIVE)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun start(
        mediaId: Long,
        mode: TrackingMode,
        counterTotal: Double?,
        counterUnit: String?,
    ): Long = database.withTransaction {
        require(counterTotal == null || counterTotal.isFinite() && counterTotal >= 0.0) {
            "Counter total must be a non-negative number"
        }
        val now = System.currentTimeMillis()
        val libraryItemId = libraryItemDao.getOrCreate(mediaId, now)
        trackingDao.getCurrent(libraryItemId)?.session?.let { current ->
            require(current.state == TrackingState.COMPLETED || current.state == TrackingState.ABANDONED) {
                "Tracking is already in progress"
            }
            trackingDao.updateSession(current.copy(currentSlot = null, updatedAt = now))
        }
        val sessionId = trackingDao.insertSession(
            TrackingSessionEntity(
                libraryItemId = libraryItemId,
                mode = mode,
                state = TrackingState.ACTIVE,
                currentSlot = 1,
                startedAt = now,
                endedAt = null,
                legacyProgressFraction = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (mode == TrackingMode.SIMPLE_COUNTER) {
            trackingDao.upsertCounter(
                TrackingCounterEntity(
                    sessionId = sessionId,
                    current = 0.0,
                    total = counterTotal,
                    unit = counterUnit.normalized(),
                    updatedAt = now,
                ),
            )
        }
        libraryItemId
    }

    override suspend fun pause(libraryItemId: Long) {
        updateState(libraryItemId, setOf(TrackingState.ACTIVE), TrackingState.PAUSED, ended = false)
    }

    override suspend fun resume(libraryItemId: Long) {
        updateState(libraryItemId, setOf(TrackingState.PAUSED), TrackingState.ACTIVE, ended = false)
    }

    override suspend fun complete(libraryItemId: Long) {
        updateState(
            libraryItemId,
            setOf(TrackingState.ACTIVE, TrackingState.PAUSED),
            TrackingState.COMPLETED,
            ended = true,
        )
    }

    override suspend fun abandon(libraryItemId: Long) {
        updateState(
            libraryItemId,
            setOf(TrackingState.ACTIVE, TrackingState.PAUSED),
            TrackingState.ABANDONED,
            ended = true,
        )
    }

    override suspend fun updateCounter(
        libraryItemId: Long,
        current: Double,
        total: Double?,
        unit: String?,
    ) {
        require(current.isFinite() && current >= 0.0) { "Counter value must be a non-negative number" }
        require(total == null || total.isFinite() && total >= 0.0) {
            "Counter total must be a non-negative number"
        }
        database.withTransaction {
            val session = requireCurrent(libraryItemId, TrackingMode.SIMPLE_COUNTER)
            val now = System.currentTimeMillis()
            trackingDao.upsertCounter(
                TrackingCounterEntity(
                    sessionId = session.id,
                    current = current,
                    total = total,
                    unit = unit.normalized(),
                    updatedAt = now,
                ),
            )
            trackingDao.updateSession(
                session.copy(legacyProgressFraction = null, updatedAt = now),
            )
        }
    }

    override suspend fun addCheckpoint(
        libraryItemId: Long,
        label: String,
        parentId: Long?,
        kind: TrackingCheckpointKind,
        origin: TrackingCheckpointOrigin,
        stableKey: String?,
    ): Long = database.withTransaction {
        require(label.isNotBlank()) { "Checkpoint label cannot be blank" }
        val session = requireCurrent(libraryItemId, TrackingMode.CHECKLIST)
        if (parentId != null) {
            require(trackingDao.getCheckpoint(parentId)?.sessionId == session.id) {
                "Checkpoint parent belongs to another session"
            }
        }
        val now = System.currentTimeMillis()
        val id = trackingDao.insertCheckpoint(
            TrackingCheckpointEntity(
                sessionId = session.id,
                stableKey = stableKey?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "user:${UUID.randomUUID()}",
                parentId = parentId,
                kind = kind,
                origin = origin,
                label = label.trim(),
                sortOrder = trackingDao.nextCheckpointSortOrder(session.id, parentId),
                completedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        trackingDao.updateSession(session.copy(legacyProgressFraction = null, updatedAt = now))
        id
    }

    override suspend fun setCheckpointCompleted(checkpointId: Long, completed: Boolean) {
        database.withTransaction {
            val checkpoint = trackingDao.getCheckpoint(checkpointId) ?: return@withTransaction
            val session = trackingDao.getSession(checkpoint.sessionId)?.session ?: return@withTransaction
            require(session.mode == TrackingMode.CHECKLIST) { "Session does not use a checklist" }
            require(session.currentSlot == 1 && session.state in EDITABLE_STATES) {
                "Tracking session has ended"
            }
            val now = System.currentTimeMillis()
            trackingDao.updateCheckpoint(
                checkpoint.copy(
                    completedAt = if (completed) checkpoint.completedAt ?: now else null,
                    updatedAt = now,
                ),
            )
            trackingDao.updateSession(session.copy(legacyProgressFraction = null, updatedAt = now))
        }
    }

    override suspend fun addQuickLog(
        libraryItemId: Long,
        note: String?,
        occurredAt: Long,
    ): Long = database.withTransaction {
        require(occurredAt > 0L) { "Quick log timestamp must be positive" }
        val session = requireCurrent(libraryItemId, TrackingMode.QUICK_LOG)
        val now = System.currentTimeMillis()
        val id = trackingDao.insertQuickLog(
            TrackingQuickLogEntity(
                sessionId = session.id,
                occurredAt = occurredAt,
                note = note.normalized(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        trackingDao.updateSession(session.copy(updatedAt = now))
        id
    }

    override suspend fun addJournalEntry(
        libraryItemId: Long,
        title: String,
        notes: String?,
        imageUri: String?,
        occurredAt: Long,
    ): Long = database.withTransaction {
        require(title.isNotBlank()) { "Journal title cannot be blank" }
        require(occurredAt > 0L) { "Journal timestamp must be positive" }
        val session = requireCurrent(libraryItemId, TrackingMode.JOURNAL)
        val now = System.currentTimeMillis()
        val id = trackingDao.insertJournalEntry(
            TrackingJournalEntryEntity(
                sessionId = session.id,
                title = title.trim(),
                occurredAt = occurredAt,
                notes = notes.normalized(),
                imageUri = imageUri.normalized(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        trackingDao.updateSession(session.copy(updatedAt = now))
        id
    }

    override suspend fun restoreCurrent(libraryItemId: Long, summary: TrackingSummary?) {
        database.withTransaction {
            val current = trackingDao.getCurrent(libraryItemId)?.session
            if (summary == null) {
                current?.let { trackingDao.deleteSession(it.id) }
                return@withTransaction
            }
            if (current != null && current.id != summary.sessionId) {
                trackingDao.deleteSession(current.id)
            }
            val existing = checkNotNull(trackingDao.getSession(summary.sessionId)?.session) {
                "Previous tracking session no longer exists"
            }
            require(existing.libraryItemId == libraryItemId && existing.mode == summary.mode) {
                "Previous tracking session does not match the library item"
            }
            trackingDao.updateSession(
                existing.copy(
                    state = summary.state,
                    currentSlot = 1,
                    startedAt = summary.startedAt,
                    endedAt = summary.endedAt,
                    updatedAt = summary.updatedAt,
                ),
            )
        }
    }

    private suspend fun updateState(
        libraryItemId: Long,
        allowedStates: Set<TrackingState>,
        state: TrackingState,
        ended: Boolean,
    ) {
        database.withTransaction {
            val current = trackingDao.getCurrent(libraryItemId)?.session ?: return@withTransaction
            if (current.state !in allowedStates) return@withTransaction
            val now = System.currentTimeMillis()
            trackingDao.updateSession(
                current.copy(
                    state = state,
                    endedAt = if (ended) now else null,
                    updatedAt = now,
                ),
            )
        }
    }

    private suspend fun requireCurrent(
        libraryItemId: Long,
        mode: TrackingMode,
    ): TrackingSessionEntity {
        val session = checkNotNull(trackingDao.getCurrent(libraryItemId)?.session) {
            "No current tracking session"
        }
        require(session.mode == mode) { "Session uses ${session.mode}" }
        require(session.state in EDITABLE_STATES) {
            "Tracking session has ended"
        }
        return session
    }

    private companion object {
        val EDITABLE_STATES = setOf(TrackingState.ACTIVE, TrackingState.PAUSED)
    }
}

internal suspend fun TrackingDao.insertDomainSession(
    session: TrackingSession,
    currentSlot: Int?,
) {
    insertSession(session.toEntity(currentSlot))
    when (val details = session.details) {
        TrackingDetails.JustEnjoying -> Unit
        is TrackingDetails.SimpleCounter -> upsertCounter(details.toEntity(session.id, session.updatedAt))
        is TrackingDetails.Checklist -> insertCheckpoints(
            details.checkpoints.parentFirst().map { it.toEntity() },
        )
        is TrackingDetails.QuickLog -> insertQuickLogs(details.entries.map { it.toEntity() })
        is TrackingDetails.Journal -> insertJournalEntries(details.entries.map { it.toEntity() })
    }
}

private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun List<TrackingCheckpoint>.parentFirst(): List<TrackingCheckpoint> {
    val ordered = mutableListOf<TrackingCheckpoint>()
    val insertedIds = mutableSetOf<Long>()
    val remaining = toMutableList()
    while (remaining.isNotEmpty()) {
        val ready = remaining.filter { it.parentId == null || it.parentId in insertedIds }
        check(ready.isNotEmpty()) { "Checkpoint hierarchy contains a cycle or missing parent" }
        ordered += ready
        insertedIds += ready.map { it.id }
        remaining.removeAll(ready.toSet())
    }
    return ordered
}
