package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.couchlist.app.core.domain.model.MediaCategory

/**
 * Catalog/metadata cache for a movie or TV show. Provider-neutral; external
 * data is sourced via [com.couchlist.app.core.data.remote.provider.MediaMetadataProvider].
 * User state lives in [LibraryItemEntity].
 */
@Entity(
    tableName = "media_items",
    indices = [Index(value = ["source", "category", "external_id"], unique = true)],
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "category")
    val category: MediaCategory,
    @ColumnInfo(name = "external_id")
    val externalId: String,
    val title: String,
    @ColumnInfo(name = "original_title")
    val originalTitle: String?,
    val description: String?,
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String?,
    @ColumnInfo(name = "backdrop_uri")
    val backdropUri: String?,
    @ColumnInfo(name = "release_date")
    val releaseDate: String?,
    @ColumnInfo(name = "original_language")
    val originalLanguage: String?,
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
