package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.couchlist.app.core.domain.model.TrackingCheckpointKind
import com.couchlist.app.core.domain.model.TrackingCheckpointOrigin
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingState

@Entity(
    tableName = "tracking_sessions",
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["library_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["library_item_id", "current_slot"], unique = true),
        Index(value = ["state", "updated_at"]),
    ],
)
data class TrackingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "library_item_id")
    val libraryItemId: Long,
    val mode: TrackingMode,
    val state: TrackingState,
    @ColumnInfo(name = "current_slot")
    val currentSlot: Int? = 1,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,
    @ColumnInfo(name = "legacy_progress_fraction")
    val legacyProgressFraction: Double?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "tracking_counters",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TrackingCounterEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "current_value")
    val current: Double,
    @ColumnInfo(name = "total_value")
    val total: Double?,
    val unit: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "tracking_checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackingCheckpointEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id", "stable_key"], unique = true),
        Index(value = ["parent_id"]),
    ],
)
data class TrackingCheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "stable_key")
    val stableKey: String,
    @ColumnInfo(name = "parent_id")
    val parentId: Long?,
    val kind: TrackingCheckpointKind,
    val origin: TrackingCheckpointOrigin,
    val label: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "tracking_quick_logs",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id", "occurred_at"])],
)
data class TrackingQuickLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    val note: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "tracking_journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id", "occurred_at"])],
)
data class TrackingJournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    val title: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    val notes: String?,
    @ColumnInfo(name = "image_uri")
    val imageUri: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

data class TrackingSessionWithDetails(
    @Embedded
    val session: TrackingSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val counters: List<TrackingCounterEntity>,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val checkpoints: List<TrackingCheckpointEntity>,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val quickLogs: List<TrackingQuickLogEntity>,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val journalEntries: List<TrackingJournalEntryEntity>,
)
