package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.WatchStatus

@Entity(
    tableName = "watchlist_items",
    indices = [Index(value = ["tmdb_id", "media_type"], unique = true)],
)
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_type")
    val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Long,
    val title: String,
    @ColumnInfo(name = "poster_path")
    val posterPath: String?,
    val status: WatchStatus,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)