package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Supplementary video metadata stored separately from [MediaItemEntity] to keep
 * the main catalog table lean. Runtime is populated on detail refresh.
 */
@Entity(
    tableName = "video_metadata",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VideoMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "runtime_minutes")
    val runtimeMinutes: Int?,
)
