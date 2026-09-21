package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaItemWithRuntimeRow
import com.couchlist.app.core.data.local.entity.VideoMetadataEntity
import com.couchlist.app.core.domain.model.MediaCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: MediaItemEntity): Long

    @Query(
        "SELECT * FROM media_items " +
            "WHERE source = :source AND category = :category AND external_id = :externalId",
    )
    suspend fun getByReference(
        source: String,
        category: MediaCategory,
        externalId: String,
    ): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query(
        "SELECT media_items.*, video_metadata.runtime_minutes " +
            "FROM media_items " +
            "LEFT JOIN video_metadata ON video_metadata.media_id = media_items.id " +
            "WHERE media_items.id = :id",
    )
    fun observeById(id: Long): Flow<MediaItemWithRuntimeRow?>

    @Query(
        "UPDATE media_items SET " +
            "title = :title, " +
            "original_title = :originalTitle, " +
            "description = :description, " +
            "artwork_uri = :artworkUri, " +
            "backdrop_uri = :backdropUri, " +
            "release_date = :releaseDate, " +
            "original_language = :originalLanguage, " +
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
        description: String?,
        artworkUri: String?,
        backdropUri: String?,
        releaseDate: String?,
        originalLanguage: String?,
        externalRating: Double,
        externalVoteCount: Long,
        genres: String?,
        refreshedAt: Long,
        updatedAt: Long,
    )

    @Upsert
    suspend fun upsertVideoMetadata(meta: VideoMetadataEntity)

    @Query("SELECT runtime_minutes FROM video_metadata WHERE media_id = :mediaId")
    suspend fun getRuntimeMinutes(mediaId: Long): Int?

    @Query(
        "SELECT media_items.genres FROM media_items " +
            "INNER JOIN log_entries ON media_items.id = log_entries.media_id " +
            "WHERE media_items.genres IS NOT NULL",
    )
    suspend fun getWatchedGenres(): List<String>

    @Query("SELECT * FROM media_items")
    suspend fun getAll(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<MediaItemEntity>): List<Long>

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()
}
