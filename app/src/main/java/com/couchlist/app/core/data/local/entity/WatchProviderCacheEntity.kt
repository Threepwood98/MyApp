package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached watch providers per media per region. Schema preparation only for now.
 */
@Entity(
    tableName = "watch_provider_cache",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["media_id"]),
        Index(value = ["media_id", "region"], unique = true),
    ],
)
data class WatchProviderCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    val region: String,
    val link: String?,
    @ColumnInfo(name = "providers_json")
    val providersJson: String?,
    @ColumnInfo(name = "refreshed_at")
    val refreshedAt: Long?,
)