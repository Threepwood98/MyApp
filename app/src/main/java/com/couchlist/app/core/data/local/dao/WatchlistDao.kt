package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.WatchlistEntity
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query(
        "SELECT * FROM watchlist_items WHERE status = :status " +
            "ORDER BY sort_order ASC, added_at DESC",
    )
    fun observeByStatus(status: WatchStatus): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: WatchlistEntity): Long

    @Query("UPDATE watchlist_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: WatchStatus)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "SELECT COUNT(*) FROM watchlist_items " +
            "WHERE tmdb_id = :tmdbId AND media_type = :mediaType",
    )
    suspend fun countByTmdb(tmdbId: Long, mediaType: MediaType): Int
}