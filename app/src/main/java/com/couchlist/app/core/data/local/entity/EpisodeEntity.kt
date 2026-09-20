package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached TV episode metadata. Episode watch state is tracked via
 * the logbook in a later phase, not on this row.
 */
@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["season_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["media_id"]),
        Index(value = ["season_id"]),
        Index(value = ["season_id", "episode_number"], unique = true),
    ],
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "season_id")
    val seasonId: Long,
    @ColumnInfo(name = "season_number")
    val seasonNumber: Int,
    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int,
    val title: String,
    val overview: String?,
    @ColumnInfo(name = "still_path")
    val stillPath: String?,
    @ColumnInfo(name = "air_date")
    val airDate: String?,
    @ColumnInfo(name = "runtime_minutes")
    val runtimeMinutes: Int?,
)
