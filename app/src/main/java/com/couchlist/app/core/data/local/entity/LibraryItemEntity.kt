package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.couchlist.app.core.domain.model.MediaStatus

/**
 * User-specific state for one media item. Exists at most once per media item;
 * list membership is tracked separately via [MediaListJoinEntity].
 */
@Entity(
    tableName = "library_items",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["media_id"], unique = true),
        Index(value = ["status"]),
    ],
)
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    val status: MediaStatus,
    val progress: Double?,
    @ColumnInfo(name = "personal_rating")
    val personalRating: Int?,
    val favorite: Boolean = false,
    val notes: String?,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "started_at")
    val startedAt: Long?,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)