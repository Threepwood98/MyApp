package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.WatchProvider
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    val supportedCategories: Set<MediaCategory>

    suspend fun search(query: String): Result<List<MediaSearchResult>>

    fun observeMedia(mediaId: Long): Flow<MediaItem?>

    suspend fun getOrCreate(searchResult: MediaSearchResult): MediaItem

    suspend fun refresh(mediaId: Long): Result<List<WatchProvider>>
}
