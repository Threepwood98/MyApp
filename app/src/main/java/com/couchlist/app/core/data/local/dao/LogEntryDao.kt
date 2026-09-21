package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.LogEntryMediaRow
import com.couchlist.app.core.data.local.entity.MonthCount
import com.couchlist.app.core.data.local.entity.RatingCount
import com.couchlist.app.core.domain.model.LogAction
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Query(
        "SELECT log_entries.*, " +
            "media_items.title AS m_title, " +
            "media_items.poster_path AS m_poster_path, " +
            "media_items.media_type AS m_media_type " +
            "FROM log_entries " +
            "INNER JOIN media_items ON log_entries.media_id = media_items.id " +
            "ORDER BY log_entries.date DESC",
    )
    fun observeAll(): Flow<List<LogEntryMediaRow>>

    @Query(
        "SELECT log_entries.*, " +
            "media_items.title AS m_title, " +
            "media_items.poster_path AS m_poster_path, " +
            "media_items.media_type AS m_media_type " +
            "FROM log_entries " +
            "INNER JOIN media_items ON log_entries.media_id = media_items.id " +
            "WHERE log_entries.action = :action " +
            "ORDER BY log_entries.date DESC",
    )
    fun observeByAction(action: LogAction): Flow<List<LogEntryMediaRow>>

    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    @Query("SELECT COUNT(*) FROM log_entries WHERE action = 'MOVIE_WATCHED'")
    fun observeMovieCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM log_entries WHERE action = 'EPISODE_WATCHED'")
    fun observeEpisodeCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT media_id) FROM log_entries")
    fun observeUniqueTitleCount(): Flow<Int>

    @Query("SELECT AVG(personal_rating) FROM log_entries WHERE personal_rating IS NOT NULL")
    fun observeAverageRating(): Flow<Double?>

    @Query(
        "SELECT personal_rating, COUNT(*) AS count " +
            "FROM log_entries WHERE personal_rating IS NOT NULL " +
            "GROUP BY personal_rating ORDER BY personal_rating",
    )
    fun observeRatingDistribution(): Flow<List<RatingCount>>

    @Query(
        "SELECT strftime('%Y-%m', date / 1000, 'unixepoch') AS month, " +
            "COUNT(DISTINCT media_id) AS count " +
            "FROM log_entries " +
            "WHERE date >= :sinceTimestamp " +
            "GROUP BY month ORDER BY month",
    )
    fun observeMonthlyActivity(sinceTimestamp: Long): Flow<List<MonthCount>>

    @Query("SELECT * FROM log_entries")
    suspend fun getAll(): List<LogEntryEntity>

    @Insert
    suspend fun insertAll(entries: List<LogEntryEntity>): List<Long>

    @Query("DELETE FROM log_entries")
    suspend fun deleteAll()
}
