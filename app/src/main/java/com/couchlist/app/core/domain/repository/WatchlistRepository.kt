package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeStatus(status: WatchStatus): Flow<List<MediaItem>>

    fun observeEntry(tmdbId: Long, mediaType: MediaType): Flow<MediaItem?>

    suspend fun addToWatchlist(
        mediaType: MediaType,
        tmdbId: Long,
        title: String,
        posterPath: String?,
    ): Long

    suspend fun moveItem(id: Long, status: WatchStatus)

    suspend fun removeItem(id: Long)

    suspend fun isInWatchlist(tmdbId: Long, mediaType: MediaType): Boolean
}