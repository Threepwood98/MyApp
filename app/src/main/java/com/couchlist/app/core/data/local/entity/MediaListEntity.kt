package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.couchlist.app.core.domain.model.MediaListType

@Entity(
    tableName = "lists",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["cover_media_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["cover_media_id"])],
)
data class MediaListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String?,
    val type: MediaListType,
    @ColumnInfo(name = "cover_media_id")
    val coverMediaId: Long?,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "smart_filter_json")
    val smartFilterJson: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
