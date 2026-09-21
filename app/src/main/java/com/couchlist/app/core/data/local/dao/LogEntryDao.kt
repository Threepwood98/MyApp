package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.LogEntryMediaRow
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
}
