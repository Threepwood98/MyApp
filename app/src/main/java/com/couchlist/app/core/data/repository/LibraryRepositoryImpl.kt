package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.LibraryRemoval
import com.couchlist.app.core.domain.model.ListMembership
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.repository.LibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val libraryItemDao: LibraryItemDao,
    private val mediaListDao: MediaListDao,
) : LibraryRepository {

    override fun observeLibrary(): Flow<List<LibraryMedia>> =
        libraryItemDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeStatus(status: MediaStatus): Flow<List<LibraryMedia>> =
        libraryItemDao.observeByStatus(status).map { rows -> rows.map { it.toDomain() } }

    override fun observeLists(): Flow<List<MediaListSummary>> =
        mediaListDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun observeEntry(mediaId: Long): Flow<LibraryItem?> =
        libraryItemDao.observeByMediaId(mediaId).map { it?.toDomain() }

    override suspend fun addToWatchlist(mediaId: Long): Long = database.withTransaction {
        val now = System.currentTimeMillis()
        val existing = libraryItemDao.getByMediaId(mediaId)
        val libraryItemId = existing?.id ?: libraryItemDao.insertIgnore(
            LibraryItemEntity(
                mediaId = mediaId,
                status = MediaStatus.BACKLOG,
                progress = null,
                personalRating = null,
                notes = null,
                startedAt = null,
                completedAt = null,
                addedAt = now,
                updatedAt = now,
            ),
        ).takeIf { it != -1L }
            ?: checkNotNull(libraryItemDao.getByMediaId(mediaId)).id

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
                mediaId = mediaId,
                addedAt = now,
            ),
        )
        libraryItemId
    }

    override suspend fun moveItem(libraryItemId: Long, status: MediaStatus) {
        val existing = libraryItemDao.getById(libraryItemId) ?: return
        val now = System.currentTimeMillis()
        libraryItemDao.updateStatus(
            id = libraryItemId,
            status = status,
            startedAt = when (status) {
                MediaStatus.WATCHING -> existing.startedAt ?: now
                else -> existing.startedAt
            },
            completedAt = when (status) {
                MediaStatus.COMPLETED -> existing.completedAt ?: now
                else -> null
            },
            updatedAt = now,
        )
    }

    override suspend fun removeItem(libraryItemId: Long): LibraryRemoval? =
        database.withTransaction {
            val existing = libraryItemDao.getById(libraryItemId) ?: return@withTransaction null
            val memberships = mediaListDao.getMemberships(existing.mediaId)
            mediaListDao.deleteMemberships(existing.mediaId)
            libraryItemDao.delete(libraryItemId)
            LibraryRemoval(
                item = existing.toDomain(),
                memberships = memberships.map { ListMembership(it.listId, it.addedAt) },
            )
        }

    override suspend fun restoreRemoval(removal: LibraryRemoval) {
        database.withTransaction {
            libraryItemDao.insertIgnore(removal.item.toEntity())
            removal.memberships.forEach { membership ->
                mediaListDao.insertJoinIgnore(
                    MediaListJoinEntity(
                        listId = membership.listId,
                        mediaId = removal.item.mediaId,
                        addedAt = membership.addedAt,
                    ),
                )
            }
        }
    }

    override suspend fun restoreItemState(item: LibraryItem) {
        libraryItemDao.update(item.toEntity())
    }

    override suspend fun isInLibrary(mediaId: Long): Boolean =
        libraryItemDao.getByMediaId(mediaId) != null

    private suspend fun ensureList(
        name: String,
        type: MediaListType,
        sortOrder: Int,
        now: Long,
    ): Long {
        mediaListDao.findListId(name, type)?.let { return it }
        val insertedId = mediaListDao.insertListIgnore(
            MediaListEntity(
                name = name,
                description = null,
                type = type,
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
