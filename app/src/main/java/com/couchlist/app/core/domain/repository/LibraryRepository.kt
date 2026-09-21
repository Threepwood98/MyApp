package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.LibraryRemoval
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaStatus
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryMedia>>

    fun observeStatus(status: MediaStatus): Flow<List<LibraryMedia>>

    fun observeLists(): Flow<List<MediaListSummary>>

    fun observeEntry(mediaId: Long): Flow<LibraryItem?>

    suspend fun addToWatchlist(mediaId: Long): Long

    suspend fun moveItem(libraryItemId: Long, status: MediaStatus)

    suspend fun removeItem(libraryItemId: Long): LibraryRemoval?

    suspend fun restoreRemoval(removal: LibraryRemoval)

    suspend fun restoreItemState(item: LibraryItem)

    suspend fun isInLibrary(mediaId: Long): Boolean

    suspend fun updateFavorite(mediaId: Long, favorite: Boolean)

    suspend fun updateRating(mediaId: Long, rating: Int?)

    suspend fun updateNotes(mediaId: Long, notes: String?)
}
