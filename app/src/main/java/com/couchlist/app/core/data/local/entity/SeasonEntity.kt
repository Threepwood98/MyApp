package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached TV season metadata.
 */
@Entity(
    tableName = "seasons",
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
        Index(value = ["media_id", "season_number"], unique = true),
    ],
)
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "season_number")
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String?,
    @ColumnInfo(name = "air_date")
    val airDate: String?,
    @ColumnInfo(name = "episode_count")
    val episodeCount: Int,
    @ColumnInfo(name = "last_refreshed_at")
    val lastRefreshedAt: Long?,
)
