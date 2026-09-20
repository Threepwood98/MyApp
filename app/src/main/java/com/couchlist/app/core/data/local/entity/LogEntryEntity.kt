package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.couchlist.app.core.domain.model.LogAction

/**
 * Append-only watch history. Multiple rows may reference the same media;
 * re-watches and re-reviews create new entries instead of overwriting.
 */
@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["media_id"]),
        Index(value = ["episode_id"]),
        Index(value = ["date"]),
    ],
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "episode_id")
    val episodeId: Long?,
    val action: LogAction,
    val date: Long,
    @ColumnInfo(name = "personal_rating")
    val personalRating: Int?,
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
