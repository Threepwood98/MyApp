package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * A [MediaItemEntity] joined with its optional [VideoMetadataEntity] runtime,
 * as returned by LEFT JOIN queries on the media_items + video_metadata tables.
 */
data class MediaItemWithRuntimeRow(
    @Embedded
    val media: MediaItemEntity,
    @ColumnInfo(name = "runtime_minutes")
    val runtimeMinutes: Int?,
)
