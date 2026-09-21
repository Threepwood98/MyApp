package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.entity.ExportLibraryItem
import com.couchlist.app.core.data.local.entity.ExportList
import com.couchlist.app.core.data.local.entity.ExportListMembership
import com.couchlist.app.core.data.local.entity.ExportLogEntry
import com.couchlist.app.core.data.local.entity.ExportMediaItem
import com.couchlist.app.core.data.local.entity.LibraryExportData
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.repository.ExportImportRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class ExportImportRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val mediaItemDao: MediaItemDao,
    private val libraryItemDao: LibraryItemDao,
    private val mediaListDao: MediaListDao,
    private val logEntryDao: LogEntryDao,
) : ExportImportRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override suspend fun exportToJson(): String {
        val mediaItems = mediaItemDao.getAll().map { it.toExport() }
        val libraryItems = libraryItemDao.getAll().map { entity ->
            val media = mediaItemDao.getById(entity.mediaId)
            entity.toExport(media)
        }
        val lists = mediaListDao.getAllLists().map { it.toExport() }
        val joins = mediaListDao.getAllJoins()
        val listMemberships = joins.mapNotNull { join ->
            val list = mediaListDao.getListById(join.listId) ?: return@mapNotNull null
            val media = mediaItemDao.getById(join.mediaId) ?: return@mapNotNull null
            ExportListMembership(
                listName = list.name,
                listType = list.type.name,
                mediaType = media.mediaType.name,
                tmdbId = media.tmdbId,
            )
        }
        val logEntries = logEntryDao.getAll().mapNotNull { entity ->
            val media = mediaItemDao.getById(entity.mediaId) ?: return@mapNotNull null
            entity.toExport(media)
        }

        val export = LibraryExportData(
            mediaItems = mediaItems,
            libraryItems = libraryItems,
            lists = lists,
            listMemberships = listMemberships,
            logEntries = logEntries,
        )
        return json.encodeToString(LibraryExportData.serializer(), export)
    }

    override suspend fun importFromJson(jsonString: String) {
        val data = json.decodeFromString(LibraryExportData.serializer(), jsonString)

        // Clear existing data in dependency order
        logEntryDao.deleteAll()
        mediaListDao.deleteAllJoins()
        mediaListDao.deleteAllLists()
        libraryItemDao.deleteAll()
        mediaItemDao.deleteAll()

        // Import media items and build TMDB ID -> local ID map
        val mediaIdMap = mutableMapOf<Pair<String, Long>, Long>()
        val entities = data.mediaItems.map { it.toEntity() }
        mediaItemDao.insertAll(entities)
        data.mediaItems.forEach { export ->
            val mediaType = MediaType.valueOf(export.mediaType)
            val entity = mediaItemDao.getByTmdb(mediaType, export.tmdbId)
            if (entity != null) {
                mediaIdMap[export.mediaType to export.tmdbId] = entity.id
            }
        }

        // Import lists and build list name+type -> local ID map
        val listIdMap = mutableMapOf<Pair<String, String>, Long>()
        val listEntities = data.lists.map { it.toEntity() }
        mediaListDao.insertAllLists(listEntities)
        data.lists.forEach { export ->
            val type = MediaListType.valueOf(export.type)
            val id = mediaListDao.findListId(export.name, type)
            if (id != null) {
                listIdMap[export.name to export.type] = id
            }
        }

        // Import library items
        val libraryEntities = data.libraryItems.mapNotNull { export ->
            val localMediaId = mediaIdMap[export.mediaType to export.tmdbId]
                ?: return@mapNotNull null
            export.toEntity(localMediaId)
        }
        libraryItemDao.insertAll(libraryEntities)

        // Import list memberships
        val joins = data.listMemberships.mapNotNull { export ->
            val localListId = listIdMap[export.listName to export.listType]
                ?: return@mapNotNull null
            val localMediaId = mediaIdMap[export.mediaType to export.tmdbId]
                ?: return@mapNotNull null
            MediaListJoinEntity(
                listId = localListId,
                mediaId = localMediaId,
                addedAt = System.currentTimeMillis(),
            )
        }
        mediaListDao.insertAllJoins(joins)

        // Import log entries
        val logEntities = data.logEntries.mapNotNull { export ->
            val localMediaId = mediaIdMap[export.mediaType to export.tmdbId]
                ?: return@mapNotNull null
            export.toEntity(localMediaId)
        }
        logEntryDao.insertAll(logEntities)
    }

    private fun MediaItemEntity.toExport() = ExportMediaItem(
        mediaType = mediaType.name,
        tmdbId = tmdbId,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        originalLanguage = originalLanguage,
        runtimeMinutes = runtimeMinutes,
        externalRating = externalRating,
        externalVoteCount = externalVoteCount,
        genres = genres,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ExportMediaItem.toEntity() = MediaItemEntity(
        mediaType = MediaType.valueOf(mediaType),
        tmdbId = tmdbId,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        originalLanguage = originalLanguage,
        runtimeMinutes = runtimeMinutes,
        externalRating = externalRating,
        externalVoteCount = externalVoteCount,
        genres = genres,
        lastRefreshedAt = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun LibraryItemEntity.toExport(media: MediaItemEntity?) = ExportLibraryItem(
        mediaType = media?.mediaType?.name ?: "MOVIE",
        tmdbId = media?.tmdbId ?: 0L,
        status = status.name,
        progress = progress,
        personalRating = personalRating,
        favorite = favorite,
        notes = notes,
        addedAt = addedAt,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt,
    )

    private fun ExportLibraryItem.toEntity(localMediaId: Long) = LibraryItemEntity(
        mediaId = localMediaId,
        status = MediaStatus.valueOf(status),
        progress = progress,
        personalRating = personalRating,
        favorite = favorite,
        notes = notes,
        addedAt = addedAt,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt,
    )

    private fun MediaListEntity.toExport() = ExportList(
        name = name,
        description = description,
        type = type.name,
        isPinned = isPinned,
        sortOrder = sortOrder,
        smartFilterJson = smartFilterJson,
    )

    private fun ExportList.toEntity() = MediaListEntity(
        name = name,
        description = description,
        type = MediaListType.valueOf(type),
        coverMediaId = null,
        isPinned = isPinned,
        sortOrder = sortOrder,
        smartFilterJson = smartFilterJson,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private fun LogEntryEntity.toExport(media: MediaItemEntity) = ExportLogEntry(
        mediaType = media.mediaType.name,
        tmdbId = media.tmdbId,
        action = action.name,
        date = date,
        personalRating = personalRating,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ExportLogEntry.toEntity(localMediaId: Long) = LogEntryEntity(
        mediaId = localMediaId,
        episodeId = null,
        action = LogAction.valueOf(action),
        date = date,
        personalRating = personalRating,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
