package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.couchlist.app.core.domain.model.MediaType

/**
 * Catalog/metadata cache for a movie or TV show. Purely external data sourced
 * from TMDB, with no user state (see [LibraryItemEntity]).
 */
@Entity(
    tableName = "media_items",
    indices = [Index(value = ["media_type", "tmdb_id"], unique = true)],
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "media_type")
    val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Long,
    val title: String,
    @ColumnInfo(name = "original_title")
    val originalTitle: String?,
    val overview: String?,
    @ColumnInfo(name = "poster_path")
    val posterPath: String?,
    @ColumnInfo(name = "backdrop_path")
    val backdropPath: String?,
    @ColumnInfo(name = "release_date")
    val releaseDate: String?,
    @ColumnInfo(name = "original_language")
    val originalLanguage: String?,
    @ColumnInfo(name = "runtime_minutes")
    val runtimeMinutes: Int?,
    @ColumnInfo(name = "external_rating")
    val externalRating: Double = 0.0,
    @ColumnInfo(name = "external_vote_count")
    val externalVoteCount: Long = 0L,
    val genres: String?,
    @ColumnInfo(name = "last_refreshed_at")
    val lastRefreshedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
