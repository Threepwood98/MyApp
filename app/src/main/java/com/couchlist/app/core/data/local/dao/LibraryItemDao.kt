package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.MediaReferenceRow
import com.couchlist.app.core.domain.model.MediaStatus
import kotlinx.coroutines.flow.Flow

private const val LIBRARY_MEDIA_SELECT =
    "SELECT " +
        "media_items.id AS m_id, " +
        "media_items.source AS m_source, " +
        "media_items.category AS m_category, " +
        "media_items.external_id AS m_external_id, " +
        "media_items.title AS m_title, " +
        "media_items.original_title AS m_original_title, " +
        "media_items.description AS m_description, " +
        "media_items.artwork_uri AS m_artwork_uri, " +
        "media_items.backdrop_uri AS m_backdrop_uri, " +
        "media_items.release_date AS m_release_date, " +
        "media_items.original_language AS m_original_language, " +
        "media_items.external_rating AS m_external_rating, " +
        "media_items.external_vote_count AS m_external_vote_count, " +
        "media_items.genres AS m_genres, " +
        "media_items.last_refreshed_at AS m_last_refreshed_at, " +
        "media_items.created_at AS m_created_at, " +
        "media_items.updated_at AS m_updated_at, " +
        "video_metadata.runtime_minutes AS m_runtime_minutes, " +
        "library_items.id AS lib_id, " +
        "library_items.status AS lib_status, " +
        "library_items.progress AS lib_progress, " +
        "library_items.personal_rating AS lib_personal_rating, " +
        "library_items.favorite AS lib_favorite, " +
        "library_items.notes AS lib_notes, " +
        "library_items.added_at AS lib_added_at, " +
        "library_items.started_at AS lib_started_at, " +
        "library_items.completed_at AS lib_completed_at, " +
        "library_items.updated_at AS lib_updated_at " +
        "FROM library_items " +
        "INNER JOIN media_items ON library_items.media_id = media_items.id " +
        "LEFT JOIN video_metadata ON video_metadata.media_id = media_items.id "

@Dao
interface LibraryItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: LibraryItemEntity): Long

    @Update
    suspend fun update(item: LibraryItemEntity)

    @Query("SELECT * FROM library_items WHERE media_id = :mediaId")
    fun observeByMediaId(mediaId: Long): Flow<LibraryItemEntity?>

    @Query("SELECT * FROM library_items WHERE media_id = :mediaId")
    suspend fun getByMediaId(mediaId: Long): LibraryItemEntity?

    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getById(id: Long): LibraryItemEntity?

    @Query(LIBRARY_MEDIA_SELECT + "WHERE library_items.status = :status ORDER BY library_items.added_at DESC")
    fun observeByStatus(status: MediaStatus): Flow<List<LibraryMediaRow>>

    @Query(LIBRARY_MEDIA_SELECT + "ORDER BY library_items.added_at DESC")
    fun observeAll(): Flow<List<LibraryMediaRow>>

    @Query(
        "UPDATE library_items SET " +
            "status = :status, " +
            "started_at = :startedAt, " +
            "completed_at = :completedAt, " +
            "updated_at = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun updateStatus(
        id: Long,
        status: MediaStatus,
        startedAt: Long?,
        completedAt: Long?,
        updatedAt: Long,
    )

    @Query(
        "UPDATE library_items SET progress = :progress, updated_at = :updatedAt " +
            "WHERE media_id = :mediaId",
    )
    suspend fun updateProgress(mediaId: Long, progress: Double?, updatedAt: Long)

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "UPDATE library_items SET favorite = :favorite, updated_at = :updatedAt " +
            "WHERE media_id = :mediaId",
    )
    suspend fun updateFavorite(mediaId: Long, favorite: Boolean, updatedAt: Long)

    @Query(
        "UPDATE library_items SET personal_rating = :rating, updated_at = :updatedAt " +
            "WHERE media_id = :mediaId",
    )
    suspend fun updateRating(mediaId: Long, rating: Int?, updatedAt: Long)

    @Query(
        "UPDATE library_items SET notes = :notes, updated_at = :updatedAt " +
            "WHERE media_id = :mediaId",
    )
    suspend fun updateNotes(mediaId: Long, notes: String?, updatedAt: Long)

    @Query(
        "SELECT media_items.source, media_items.category, media_items.external_id " +
            "FROM library_items " +
            "INNER JOIN media_items ON library_items.media_id = media_items.id",
    )
    fun observeAllMediaReferences(): Flow<List<MediaReferenceRow>>

    @Query("SELECT * FROM library_items")
    suspend fun getAll(): List<LibraryItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<LibraryItemEntity>): List<Long>

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
