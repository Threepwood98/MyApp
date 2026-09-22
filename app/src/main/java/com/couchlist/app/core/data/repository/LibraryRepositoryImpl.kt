package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.dao.TrackingDao
import com.couchlist.app.core.data.local.entity.ListGroupEntity
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.LibraryRemoval
import com.couchlist.app.core.domain.model.ListMembership
import com.couchlist.app.core.domain.model.ListGroup
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.SmartFilter
import com.couchlist.app.core.domain.repository.LibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val libraryItemDao: LibraryItemDao,
    private val mediaListDao: MediaListDao,
    private val trackingDao: TrackingDao,
) : LibraryRepository {

    override fun observeLibrary(): Flow<List<LibraryMedia>> =
        libraryItemDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeLists(): Flow<List<MediaListSummary>> =
        mediaListDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun observeGroups(): Flow<List<ListGroup>> =
        mediaListDao.observeGroups().map { groups -> groups.map { it.toDomain() } }

    override fun observeList(listId: Long): Flow<MediaList?> =
        mediaListDao.observeListById(listId).map { it?.toDomain() }

    override fun observeListItems(listId: Long): Flow<List<LibraryMedia>> =
        mediaListDao.observeListById(listId).flatMapLatest { list ->
            when {
                list == null -> flowOf(emptyList())
                list.type == MediaListType.SMART_LIST -> {
                    val filter = SmartFilter.fromJson(list.smartFilterJson)
                    libraryItemDao.observeAll().map { rows ->
                        val items = rows.map { it.toDomain() }
                        if (filter == null) emptyList() else items.filter(filter::matches)
                    }
                }
                else -> mediaListDao.observeListItems(listId).map { rows ->
                    rows.map { it.toDomain() }
                }
            }
        }

    override fun observeMembershipListIds(libraryItemId: Long): Flow<Set<Long>> =
        mediaListDao.observeMembershipListIds(libraryItemId).map { it.toSet() }

    override fun observePileMediaReferences(): Flow<Set<MediaReference>> =
        mediaListDao.observePileMediaReferences().map { rows ->
            rows.mapTo(mutableSetOf()) { MediaReference(it.source, it.category, it.externalId) }
        }

    override fun observeEntry(mediaId: Long): Flow<LibraryItem?> =
        libraryItemDao.observeByMediaId(mediaId).map { it?.toDomain() }

    override suspend fun addToWatchlist(mediaId: Long): Long = database.withTransaction {
        val now = System.currentTimeMillis()
        val libraryItemId = libraryItemDao.getOrCreate(mediaId, now)

        val watchlistId = ensureList(
            name = DEFAULT_WATCHLIST_NAME,
            type = MediaListType.TODO,
            sortOrder = 0,
            now = now,
        )
        ensureList(
            name = DEFAULT_PILE_NAME,
            type = MediaListType.PILE,
            sortOrder = 1,
            now = now,
        )
        mediaListDao.insertJoinIgnore(
            MediaListJoinEntity(
                listId = watchlistId,
                libraryItemId = libraryItemId,
                addedAt = now,
            ),
        )
        libraryItemId
    }

    override suspend fun addToPile(mediaId: Long): Long = database.withTransaction {
        val now = System.currentTimeMillis()
        val libraryItemId = libraryItemDao.getOrCreate(mediaId, now)
        val pileId = ensureList(
            name = DEFAULT_PILE_NAME,
            type = MediaListType.PILE,
            sortOrder = 0,
            now = now,
        )
        mediaListDao.insertJoinIgnore(
            MediaListJoinEntity(
                listId = pileId,
                libraryItemId = libraryItemId,
                addedAt = now,
            ),
        )
        libraryItemId
    }

    override suspend fun removeItem(libraryItemId: Long): LibraryRemoval? =
        database.withTransaction {
            val existing = libraryItemDao.getById(libraryItemId) ?: return@withTransaction null
            val memberships = mediaListDao.getMemberships(existing.id)
            val trackingSessions = trackingDao.getSessions(existing.id).map { it.toDomain() }
            libraryItemDao.delete(libraryItemId)
            LibraryRemoval(
                item = existing.toDomain(),
                memberships = memberships.map { ListMembership(it.listId, it.addedAt) },
                trackingSessions = trackingSessions,
            )
        }

    override suspend fun restoreRemoval(removal: LibraryRemoval) {
        database.withTransaction {
            val insertedId = libraryItemDao.insertIgnore(removal.item.toEntity())
            val libraryItemId = insertedId.takeIf { it != -1L }
                ?: checkNotNull(libraryItemDao.getByMediaId(removal.item.mediaId)).id
            removal.memberships.forEach { membership ->
                mediaListDao.insertJoinIgnore(
                    MediaListJoinEntity(
                        listId = membership.listId,
                        libraryItemId = libraryItemId,
                        addedAt = membership.addedAt,
                    ),
                )
            }
            removal.trackingSessions.forEach { session ->
                trackingDao.insertDomainSession(
                    session = session.copy(libraryItemId = libraryItemId),
                    currentSlot = if (session.isCurrent) 1 else null,
                )
            }
        }
    }

    override suspend fun isInLibrary(mediaId: Long): Boolean =
        libraryItemDao.getByMediaId(mediaId) != null

    override suspend fun updateFavorite(mediaId: Long, favorite: Boolean) {
        libraryItemDao.updateFavorite(mediaId, favorite, System.currentTimeMillis())
    }

    override suspend fun updateRating(mediaId: Long, rating: Int?) {
        libraryItemDao.updateRating(mediaId, rating, System.currentTimeMillis())
    }

    override suspend fun updateNotes(mediaId: Long, notes: String?) {
        libraryItemDao.updateNotes(mediaId, notes, System.currentTimeMillis())
    }

    override fun observeAllMediaReferences(): Flow<Set<MediaReference>> =
        libraryItemDao.observeAllMediaReferences().map { rows ->
            rows.mapTo(mutableSetOf()) { row ->
                MediaReference(row.source, row.category, row.externalId)
            }
        }

    override suspend fun createList(
        name: String,
        description: String?,
        type: MediaListType,
        smartFilterJson: String?,
        groupId: Long?,
    ): Long {
        require(type != MediaListType.PILE) { "The Pile is managed by the app" }
        require(name.isNotBlank()) { "List name cannot be blank" }
        val now = System.currentTimeMillis()
        val insertedId = mediaListDao.insertListIgnore(
            MediaListEntity(
                name = name.trim(),
                description = description?.trim()?.takeIf(String::isNotEmpty),
                type = type,
                groupId = groupId,
                coverMediaId = null,
                isPinned = false,
                sortOrder = mediaListDao.nextListSortOrder(),
                smartFilterJson = smartFilterJson,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return insertedId.takeIf { it != -1L }
            ?: checkNotNull(mediaListDao.findListId(name, type))
    }

    override suspend fun updateList(
        listId: Long,
        name: String,
        description: String?,
        type: MediaListType,
        groupId: Long?,
    ) {
        val existing = mediaListDao.getListById(listId) ?: return
        if (existing.type == MediaListType.PILE) return
        require(type != MediaListType.PILE) { "Only the app can create The Pile" }
        require(name.isNotBlank()) { "List name cannot be blank" }
        mediaListDao.updateList(
            listId = listId,
            name = name.trim(),
            description = description?.trim()?.takeIf(String::isNotEmpty),
            type = type,
            groupId = groupId,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun duplicateList(listId: Long): Long? = database.withTransaction {
        val source = mediaListDao.getListById(listId) ?: return@withTransaction null
        if (source.type == MediaListType.PILE) return@withTransaction null
        val now = System.currentTimeMillis()
        val duplicateId = mediaListDao.insertListIgnore(
            source.copy(
                id = 0L,
                name = "${source.name} copy",
                isPinned = false,
                sortOrder = mediaListDao.nextListSortOrder(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (duplicateId == -1L) return@withTransaction null
        mediaListDao.getListMemberships(source.id).forEach { membership ->
            mediaListDao.insertJoinIgnore(
                membership.copy(id = 0L, listId = duplicateId, addedAt = now),
            )
        }
        duplicateId
    }

    override suspend fun setListPinned(listId: Long, isPinned: Boolean) {
        val list = mediaListDao.getListById(listId) ?: return
        if (list.type == MediaListType.PILE) return
        mediaListDao.updatePinned(listId, isPinned, System.currentTimeMillis())
    }

    override suspend fun setListGroup(listId: Long, groupId: Long?) {
        val list = mediaListDao.getListById(listId) ?: return
        if (list.type == MediaListType.PILE) return
        mediaListDao.updateGroup(listId, groupId, System.currentTimeMillis())
    }

    override suspend fun reorderLists(listIds: List<Long>) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            listIds.forEachIndexed { index, listId ->
                mediaListDao.updateListSortOrder(listId, index, now)
            }
        }
    }

    override suspend fun deleteList(listId: Long) {
        if (mediaListDao.getListById(listId)?.type == MediaListType.PILE) return
        mediaListDao.deleteList(listId)
    }

    override suspend fun addToList(listId: Long, mediaId: Long): Long = database.withTransaction {
        val list = checkNotNull(mediaListDao.getListById(listId)) { "Unknown list" }
        require(list.type != MediaListType.SMART_LIST) { "Smart lists have dynamic membership" }
        val now = System.currentTimeMillis()
        val libraryItemId = libraryItemDao.getOrCreate(mediaId, now)
        mediaListDao.insertJoinIgnore(MediaListJoinEntity(listId = listId, libraryItemId = libraryItemId, addedAt = now))
        libraryItemId
    }

    override suspend fun removeFromList(listId: Long, libraryItemId: Long) {
        mediaListDao.removeFromList(listId, libraryItemId)
    }

    override suspend fun copyToList(targetListId: Long, libraryItemId: Long) {
        val target = mediaListDao.getListById(targetListId) ?: return
        if (target.type == MediaListType.SMART_LIST) return
        mediaListDao.insertJoinIgnore(
            MediaListJoinEntity(
                listId = targetListId,
                libraryItemId = libraryItemId,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun moveToList(sourceListId: Long, targetListId: Long, libraryItemId: Long) {
        database.withTransaction {
            val target = mediaListDao.getListById(targetListId) ?: return@withTransaction
            if (target.type == MediaListType.SMART_LIST) return@withTransaction
            mediaListDao.insertJoinIgnore(
                MediaListJoinEntity(
                    listId = targetListId,
                    libraryItemId = libraryItemId,
                    addedAt = System.currentTimeMillis(),
                ),
            )
            mediaListDao.removeFromList(sourceListId, libraryItemId)
        }
    }

    override fun observeListMediaIds(listId: Long): Flow<List<Long>> =
        mediaListDao.observeListMediaIds(listId)

    override suspend fun getListName(listId: Long): String? =
        mediaListDao.getListById(listId)?.name

    override suspend fun getSmartFilterJson(listId: Long): String? =
        mediaListDao.getListById(listId)?.smartFilterJson

    override fun observeSmartListItems(listId: Long): Flow<List<LibraryMedia>> =
        observeListItems(listId)

    override suspend fun createGroup(name: String): Long {
        require(name.isNotBlank()) { "Group name cannot be blank" }
        val now = System.currentTimeMillis()
        return mediaListDao.insertGroup(
            ListGroupEntity(
                name = name.trim(),
                sortOrder = mediaListDao.nextGroupSortOrder(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun renameGroup(groupId: Long, name: String) {
        require(name.isNotBlank()) { "Group name cannot be blank" }
        mediaListDao.updateGroupName(groupId, name.trim(), System.currentTimeMillis())
    }

    override suspend fun deleteGroup(groupId: Long) {
        mediaListDao.deleteGroup(groupId)
    }

    override suspend fun reorderGroups(groupIds: List<Long>) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            groupIds.forEachIndexed { index, groupId ->
                mediaListDao.updateGroupSortOrder(groupId, index, now)
            }
        }
    }

    private suspend fun ensureList(
        name: String,
        type: MediaListType,
        sortOrder: Int,
        now: Long,
    ): Long {
        if (type == MediaListType.PILE) {
            mediaListDao.getPile()?.let { return it.id }
        }
        mediaListDao.findListId(name, type)?.let { return it }
        val insertedId = mediaListDao.insertListIgnore(
            MediaListEntity(
                name = name,
                description = null,
                type = type,
                groupId = null,
                coverMediaId = null,
                isPinned = true,
                sortOrder = sortOrder,
                smartFilterJson = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return insertedId.takeIf { it != -1L }
            ?: checkNotNull(mediaListDao.findListId(name, type))
    }

    private companion object {
        const val DEFAULT_WATCHLIST_NAME = "Watchlist"
        const val DEFAULT_PILE_NAME = "The Pile"
    }
}
