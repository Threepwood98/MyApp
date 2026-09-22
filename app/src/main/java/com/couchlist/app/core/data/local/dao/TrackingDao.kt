package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.couchlist.app.core.data.local.entity.TrackingCheckpointEntity
import com.couchlist.app.core.data.local.entity.TrackingCounterEntity
import com.couchlist.app.core.data.local.entity.TrackingJournalEntryEntity
import com.couchlist.app.core.data.local.entity.TrackingQuickLogEntity
import com.couchlist.app.core.data.local.entity.TrackingSessionEntity
import com.couchlist.app.core.data.local.entity.TrackingSessionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Transaction
    @Query("SELECT * FROM tracking_sessions WHERE library_item_id = :libraryItemId AND current_slot = 1 LIMIT 1")
    fun observeCurrent(libraryItemId: Long): Flow<TrackingSessionWithDetails?>

    @Transaction
    @Query(
        "SELECT tracking_sessions.* FROM tracking_sessions " +
            "INNER JOIN library_items ON library_items.id = tracking_sessions.library_item_id " +
            "WHERE library_items.media_id = :mediaId AND tracking_sessions.current_slot = 1 LIMIT 1",
    )
    fun observeCurrentByMediaId(mediaId: Long): Flow<TrackingSessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM tracking_sessions WHERE library_item_id = :libraryItemId AND current_slot = 1 LIMIT 1")
    suspend fun getCurrent(libraryItemId: Long): TrackingSessionWithDetails?

    @Transaction
    @Query("SELECT * FROM tracking_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): TrackingSessionWithDetails?

    @Transaction
    @Query("SELECT * FROM tracking_sessions WHERE library_item_id = :libraryItemId ORDER BY created_at ASC, id ASC")
    suspend fun getSessions(libraryItemId: Long): List<TrackingSessionWithDetails>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: TrackingSessionEntity): Long

    @Update
    suspend fun updateSession(session: TrackingSessionEntity)

    @Query("DELETE FROM tracking_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: TrackingCounterEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCheckpoint(checkpoint: TrackingCheckpointEntity): Long

    @Update
    suspend fun updateCheckpoint(checkpoint: TrackingCheckpointEntity)

    @Query("SELECT * FROM tracking_checkpoints WHERE id = :checkpointId")
    suspend fun getCheckpoint(checkpointId: Long): TrackingCheckpointEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM tracking_checkpoints WHERE session_id = :sessionId AND parent_id IS :parentId")
    suspend fun nextCheckpointSortOrder(sessionId: Long, parentId: Long?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuickLog(entry: TrackingQuickLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalEntry(entry: TrackingJournalEntryEntity): Long

    @Query("SELECT * FROM tracking_sessions ORDER BY id")
    suspend fun getAllSessions(): List<TrackingSessionEntity>

    @Query("SELECT * FROM tracking_counters ORDER BY session_id")
    suspend fun getAllCounters(): List<TrackingCounterEntity>

    @Query("SELECT * FROM tracking_checkpoints ORDER BY id")
    suspend fun getAllCheckpoints(): List<TrackingCheckpointEntity>

    @Query("SELECT * FROM tracking_quick_logs ORDER BY id")
    suspend fun getAllQuickLogs(): List<TrackingQuickLogEntity>

    @Query("SELECT * FROM tracking_journal_entries ORDER BY id")
    suspend fun getAllJournalEntries(): List<TrackingJournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCounters(counters: List<TrackingCounterEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCheckpoints(checkpoints: List<TrackingCheckpointEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuickLogs(entries: List<TrackingQuickLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalEntries(entries: List<TrackingJournalEntryEntity>): List<Long>

    @Query("DELETE FROM tracking_journal_entries")
    suspend fun deleteAllJournalEntries()

    @Query("DELETE FROM tracking_quick_logs")
    suspend fun deleteAllQuickLogs()

    @Query("DELETE FROM tracking_checkpoints")
    suspend fun deleteAllCheckpoints()

    @Query("DELETE FROM tracking_counters")
    suspend fun deleteAllCounters()

    @Query("DELETE FROM tracking_sessions")
    suspend fun deleteAllSessions()
}
