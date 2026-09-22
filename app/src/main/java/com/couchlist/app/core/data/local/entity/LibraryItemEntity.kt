package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    ],
)
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "personal_rating")
    val personalRating: Int?,
    val favorite: Boolean = false,
    val notes: String?,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
