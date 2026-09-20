package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Membership of a media item in a list (many-to-many). A media item may be in
 * any number of lists; a list may group any number of items.
 */
@Entity(
    tableName = "media_list_joins",
    foreignKeys = [
        ForeignKey(
            entity = MediaListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["list_id"]),
        Index(value = ["media_id"]),
        Index(value = ["list_id", "media_id"], unique = true),
    ],
)
data class MediaListJoinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "list_id")
    val listId: Long,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)