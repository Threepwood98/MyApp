package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.LibraryRemoval
import com.couchlist.app.core.domain.model.ListGroup
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaReference
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryMedia>>

    fun observeLists(): Flow<List<MediaListSummary>>

    fun observeGroups(): Flow<List<ListGroup>>

    fun observeList(listId: Long): Flow<MediaList?>

    fun observeListItems(listId: Long): Flow<List<LibraryMedia>>

    fun observeMembershipListIds(libraryItemId: Long): Flow<Set<Long>>

    fun observePileMediaReferences(): Flow<Set<MediaReference>>

    fun observeEntry(mediaId: Long): Flow<LibraryItem?>

    suspend fun addToWatchlist(mediaId: Long): Long

    suspend fun addToPile(mediaId: Long): Long

    suspend fun removeItem(libraryItemId: Long): LibraryRemoval?

    suspend fun restoreRemoval(removal: LibraryRemoval)

    suspend fun isInLibrary(mediaId: Long): Boolean

    suspend fun updateFavorite(mediaId: Long, favorite: Boolean)

    suspend fun updateRating(mediaId: Long, rating: Int?)

    suspend fun updateNotes(mediaId: Long, notes: String?)

    fun observeAllMediaReferences(): Flow<Set<MediaReference>>

    suspend fun createList(
        name: String,
        description: String?,
        type: MediaListType,
        smartFilterJson: String? = null,
        groupId: Long? = null,
    ): Long

    suspend fun updateList(
        listId: Long,
        name: String,
        description: String?,
        type: MediaListType,
        groupId: Long?,
    )

    suspend fun duplicateList(listId: Long): Long?

    suspend fun setListPinned(listId: Long, isPinned: Boolean)

    suspend fun setListGroup(listId: Long, groupId: Long?)

    suspend fun reorderLists(listIds: List<Long>)

    suspend fun deleteList(listId: Long)

    suspend fun addToList(listId: Long, mediaId: Long): Long

    suspend fun removeFromList(listId: Long, libraryItemId: Long)

    suspend fun copyToList(targetListId: Long, libraryItemId: Long)

    suspend fun moveToList(sourceListId: Long, targetListId: Long, libraryItemId: Long)

    fun observeListMediaIds(listId: Long): Flow<List<Long>>

    suspend fun getListName(listId: Long): String?

    suspend fun getSmartFilterJson(listId: Long): String?

    fun observeSmartListItems(listId: Long): Flow<List<LibraryMedia>>

    suspend fun createGroup(name: String): Long

    suspend fun renameGroup(groupId: Long, name: String)

    suspend fun deleteGroup(groupId: Long)

    suspend fun reorderGroups(groupIds: List<Long>)
}
