package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Membership of a library item in a list (many-to-many). User state and list
 * membership therefore share the same lifecycle.
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
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["library_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["list_id"]),
        Index(value = ["library_item_id"]),
        Index(value = ["list_id", "library_item_id"], unique = true),
    ],
)
data class MediaListJoinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "list_id")
    val listId: Long,
    @ColumnInfo(name = "library_item_id")
    val libraryItemId: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)
