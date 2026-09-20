package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: MediaItemEntity): Long

    @Query(
        "SELECT * FROM media_items " +
            "WHERE media_type = :mediaType AND tmdb_id = :tmdbId",
    )
    suspend fun getByTmdb(mediaType: MediaType, tmdbId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun observeById(id: Long): Flow<MediaItemEntity?>

    @Query(
        "UPDATE media_items SET " +
            "title = :title, " +
            "original_title = :originalTitle, " +
            "overview = :overview, " +
            "poster_path = :posterPath, " +
            "backdrop_path = :backdropPath, " +
            "release_date = :releaseDate, " +
            "original_language = :originalLanguage, " +
            "runtime_minutes = :runtimeMinutes, " +
            "external_rating = :externalRating, " +
            "external_vote_count = :externalVoteCount, " +
            "genres = :genres, " +
            "last_refreshed_at = :refreshedAt, " +
            "updated_at = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun updateMetadata(
        id: Long,
        title: String,
        originalTitle: String?,
        overview: String?,
        posterPath: String?,
        backdropPath: String?,
        releaseDate: String?,
        originalLanguage: String?,
        runtimeMinutes: Int?,
        externalRating: Double,
        externalVoteCount: Long,
        genres: String?,
        refreshedAt: Long,
        updatedAt: Long,
    )
}
