package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.LogEntry
import com.couchlist.app.core.domain.model.LogMediaEntry
import kotlinx.coroutines.flow.Flow

interface LogbookRepository {
    fun observeAll(): Flow<List<LogMediaEntry>>

    fun observeByAction(action: LogAction): Flow<List<LogMediaEntry>>

    suspend fun logWatched(
        mediaId: Long,
        episodeId: Long? = null,
        rating: Int? = null,
        notes: String? = null,
    ): Long
}
