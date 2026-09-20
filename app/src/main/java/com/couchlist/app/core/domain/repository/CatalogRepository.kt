package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.WatchProvider
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeMedia(mediaId: Long): Flow<MediaItem?>

    suspend fun getOrCreate(searchResult: MediaSearchResult): MediaItem

    suspend fun refresh(mediaId: Long): Result<List<WatchProvider>>
}
