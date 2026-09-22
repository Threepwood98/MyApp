package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.MediaReferenceRow
import com.couchlist.app.core.domain.model.TrackingState
import kotlinx.coroutines.flow.Flow

internal const val LIBRARY_MEDIA_COLUMNS =
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
        "library_items.personal_rating AS lib_personal_rating, " +
        "library_items.favorite AS lib_favorite, " +
        "library_items.notes AS lib_notes, " +
        "library_items.added_at AS lib_added_at, " +
        "library_items.updated_at AS lib_updated_at, " +
        "tracking_sessions.id AS tracking_session_id, " +
        "tracking_sessions.mode AS tracking_mode, " +
        "tracking_sessions.state AS tracking_state, " +
        "tracking_sessions.started_at AS tracking_started_at, " +
        "tracking_sessions.ended_at AS tracking_ended_at, " +
        "tracking_sessions.updated_at AS tracking_updated_at, " +
        "tracking_sessions.legacy_progress_fraction AS tracking_legacy_progress, " +
        "tracking_counters.current_value AS tracking_counter_current, " +
        "tracking_counters.total_value AS tracking_counter_total, " +
        "tracking_checkpoint_progress.total_count AS tracking_checkpoint_total, " +
        "tracking_checkpoint_progress.completed_count AS tracking_checkpoint_completed "

private const val LIBRARY_MEDIA_SELECT =
    LIBRARY_MEDIA_COLUMNS +
        "FROM library_items " +
        "INNER JOIN media_items ON library_items.media_id = media_items.id " +
        "LEFT JOIN video_metadata ON video_metadata.media_id = media_items.id " +
        "LEFT JOIN tracking_sessions ON tracking_sessions.library_item_id = library_items.id " +
        "AND tracking_sessions.current_slot = 1 " +
        "LEFT JOIN tracking_counters ON tracking_counters.session_id = tracking_sessions.id " +
        "LEFT JOIN (" +
        "SELECT session_id, COUNT(*) AS total_count, " +
        "SUM(CASE WHEN completed_at IS NOT NULL THEN 1 ELSE 0 END) AS completed_count " +
        "FROM tracking_checkpoints WHERE kind = 'ITEM' GROUP BY session_id" +
        ") tracking_checkpoint_progress " +
        "ON tracking_checkpoint_progress.session_id = tracking_sessions.id "

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

    @Query(
        LIBRARY_MEDIA_SELECT +
            "WHERE tracking_sessions.state = :state ORDER BY tracking_sessions.updated_at DESC",
    )
    fun observeByTrackingState(state: TrackingState): Flow<List<LibraryMediaRow>>

    @Query(LIBRARY_MEDIA_SELECT + "ORDER BY library_items.added_at DESC")
    fun observeAll(): Flow<List<LibraryMediaRow>>

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
