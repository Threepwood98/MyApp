package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.dao.WatchlistDao
import com.couchlist.app.core.data.local.entity.WatchlistEntity
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.WatchStatus
import com.couchlist.app.core.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
) : WatchlistRepository {

    override fun observeStatus(status: WatchStatus): Flow<List<MediaItem>> =
        dao.observeByStatus(status).map { entities -> entities.map { it.toDomain() } }

    override fun observeEntry(tmdbId: Long, mediaType: MediaType): Flow<MediaItem?> =
        dao.observeByTmdb(tmdbId, mediaType).map { it?.toDomain() }

    override suspend fun addToWatchlist(
        mediaType: MediaType,
        tmdbId: Long,
        title: String,
        posterPath: String?,
    ): Long = dao.insert(
        WatchlistEntity(
            mediaType = mediaType,
            tmdbId = tmdbId,
            title = title,
            posterPath = posterPath,
            status = WatchStatus.WATCHLIST,
        ),
    )

    override suspend fun moveItem(id: Long, status: WatchStatus) {
        dao.updateStatus(id, status)
    }

    override suspend fun removeItem(id: Long) {
        dao.delete(id)
    }

    override suspend fun isInWatchlist(tmdbId: Long, mediaType: MediaType): Boolean =
        dao.countByTmdb(tmdbId, mediaType) > 0

    private fun WatchlistEntity.toDomain() = MediaItem(
        id = id,
        mediaType = mediaType,
        tmdbId = tmdbId,
        title = title,
        posterPath = posterPath,
        status = status,
        sortOrder = sortOrder,
        addedAt = addedAt,
    )
}