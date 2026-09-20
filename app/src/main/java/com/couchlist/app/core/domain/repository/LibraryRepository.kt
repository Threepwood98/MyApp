package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaStatus
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeStatus(status: MediaStatus): Flow<List<LibraryMedia>>

    fun observeEntry(mediaId: Long): Flow<LibraryItem?>

    suspend fun addToWatchlist(mediaId: Long): Long

    suspend fun moveItem(libraryItemId: Long, status: MediaStatus)

    suspend fun removeItem(libraryItemId: Long)

    suspend fun isInLibrary(mediaId: Long): Boolean
}
