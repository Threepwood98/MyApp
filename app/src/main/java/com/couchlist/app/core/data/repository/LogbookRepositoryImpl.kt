package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.LogEntry
import com.couchlist.app.core.domain.model.LogMediaEntry
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.repository.LogbookRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogbookRepositoryImpl @Inject constructor(
    private val logEntryDao: LogEntryDao,
) : LogbookRepository {

    override fun observeAll(): Flow<List<LogMediaEntry>> =
        logEntryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByAction(action: LogAction): Flow<List<LogMediaEntry>> =
        logEntryDao.observeByAction(action).map { rows -> rows.map { it.toDomain() } }

    override suspend fun logWatched(
        mediaId: Long,
        episodeId: Long?,
        rating: Int?,
        notes: String?,
    ): Long {
        val now = System.currentTimeMillis()
        return logEntryDao.insert(
            LogEntryEntity(
                mediaId = mediaId,
                episodeId = episodeId,
                action = if (episodeId != null) LogAction.EPISODE_WATCHED else LogAction.MOVIE_WATCHED,
                date = now,
                personalRating = rating,
                notes = notes,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private fun com.couchlist.app.core.data.local.entity.LogEntryMediaRow.toDomain() =
        LogMediaEntry(
            entry = LogEntry(
                id = logEntry.id,
                mediaItemId = logEntry.mediaId,
                episodeId = logEntry.episodeId,
                action = logEntry.action,
                date = logEntry.date,
                personalRating = logEntry.personalRating,
                notes = logEntry.notes,
                createdAt = logEntry.createdAt,
                updatedAt = logEntry.updatedAt,
            ),
            mediaTitle = mediaTitle,
            artworkUri = artworkUri,
            category = MediaCategory.valueOf(category),
        )
}
