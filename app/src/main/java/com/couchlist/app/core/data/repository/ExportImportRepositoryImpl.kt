package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
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
import com.couchlist.app.core.data.local.entity.VideoMetadataEntity
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
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
        val mediaItems = mediaItemDao.getAll().map { entity ->
            val runtime = mediaItemDao.getRuntimeMinutes(entity.id)
            entity.toExport(runtime)
        }
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
                source = media.source,
                category = media.category.name,
                externalId = media.externalId,
                listName = list.name,
                listType = list.type.name,
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
        validateImportData(data)

        database.withTransaction {
            logEntryDao.deleteAll()
            mediaListDao.deleteAllJoins()
            mediaListDao.deleteAllLists()
            libraryItemDao.deleteAll()
            mediaItemDao.deleteAll()

            val mediaIdMap = mutableMapOf<String, Long>()
            val entities = data.mediaItems.map { it.toEntity() }
            mediaItemDao.insertAll(entities)
            data.mediaItems.forEach { export ->
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val entity = mediaItemDao.getByReference(source, category, externalId)
                if (entity != null) {
                    mediaIdMap[referenceKey(source, category, externalId)] = entity.id
                }
            }

            // Import video metadata (runtime) so round-trips preserve runtime_minutes.
            data.mediaItems.filter { it.runtimeMinutes != null }.forEach { export ->
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val localMediaId = mediaIdMap[referenceKey(source, category, externalId)]
                    ?: return@forEach
                mediaItemDao.upsertVideoMetadata(
                    VideoMetadataEntity(mediaId = localMediaId, runtimeMinutes = export.runtimeMinutes),
                )
            }

            // Import lists and build list name+type -> local ID map.
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

            // Import library items.
            val libraryEntities = data.libraryItems.mapNotNull { export ->
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val localMediaId = mediaIdMap[referenceKey(source, category, externalId)]
                    ?: return@mapNotNull null
                export.toEntity(localMediaId)
            }
            libraryItemDao.insertAll(libraryEntities)

            // Import list memberships.
            val joins = data.listMemberships.mapNotNull { export ->
                val localListId = listIdMap[export.listName to export.listType]
                    ?: return@mapNotNull null
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val localMediaId = mediaIdMap[referenceKey(source, category, externalId)]
                    ?: return@mapNotNull null
                MediaListJoinEntity(
                    listId = localListId,
                    mediaId = localMediaId,
                    addedAt = System.currentTimeMillis(),
                )
            }
            mediaListDao.insertAllJoins(joins)

            // Import log entries.
            val logEntities = data.logEntries.mapNotNull { export ->
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val localMediaId = mediaIdMap[referenceKey(source, category, externalId)]
                    ?: return@mapNotNull null
                export.toEntity(localMediaId)
            }
            logEntryDao.insertAll(logEntities)
        }
    }

    private fun validateImportData(data: LibraryExportData) {
        data.mediaItems.forEach { item ->
            item.resolvedSource()
            MediaCategory.valueOf(item.resolvedCategory())
            item.resolvedExternalId()
        }
        data.libraryItems.forEach { item ->
            item.resolvedSource()
            MediaCategory.valueOf(item.resolvedCategory())
            item.resolvedExternalId()
            MediaStatus.valueOf(item.status)
        }
        data.lists.forEach { MediaListType.valueOf(it.type) }
        data.logEntries.forEach { entry ->
            entry.resolvedSource()
            MediaCategory.valueOf(entry.resolvedCategory())
            entry.resolvedExternalId()
            LogAction.valueOf(entry.action)
        }
    }

    private fun normalizeArtworkUri(uri: String?): String? = when {
        uri == null -> null
        uri.startsWith("http://") || uri.startsWith("https://") -> uri
        else -> "https://image.tmdb.org/t/p/w500$uri"
    }

    private fun normalizeBackdropUri(uri: String?): String? = when {
        uri == null -> null
        uri.startsWith("http://") || uri.startsWith("https://") -> uri
        else -> "https://image.tmdb.org/t/p/w780$uri"
    }

    private fun MediaItemEntity.toExport(runtimeMinutes: Int? = null) = ExportMediaItem(
        source = source,
        category = category.name,
        externalId = externalId,
        title = title,
        originalTitle = originalTitle,
        description = description,
        artworkUri = artworkUri,
        backdropUri = backdropUri,
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
        source = resolvedSource(),
        category = MediaCategory.valueOf(resolvedCategory()),
        externalId = resolvedExternalId(),
        title = title,
        originalTitle = originalTitle,
        description = description ?: overview,
        artworkUri = normalizeArtworkUri(artworkUri ?: posterPath),
        backdropUri = normalizeBackdropUri(backdropUri ?: backdropPath),
        releaseDate = releaseDate,
        originalLanguage = originalLanguage,
        externalRating = externalRating,
        externalVoteCount = externalVoteCount,
        genres = genres,
        lastRefreshedAt = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun LibraryItemEntity.toExport(media: MediaItemEntity?) = ExportLibraryItem(
        source = media?.source,
        category = media?.category?.name,
        externalId = media?.externalId,
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
        source = media.source,
        category = media.category.name,
        externalId = media.externalId,
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

    private fun referenceKey(source: String, category: MediaCategory, externalId: String): String =
        "$source:${category.name}:$externalId"
}
