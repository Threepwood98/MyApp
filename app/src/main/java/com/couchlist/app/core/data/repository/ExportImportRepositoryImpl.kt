package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.entity.ExportLibraryItem
import com.couchlist.app.core.data.local.entity.ExportList
import com.couchlist.app.core.data.local.entity.ExportListGroup
import com.couchlist.app.core.data.local.entity.ExportListMembership
import com.couchlist.app.core.data.local.entity.ExportLogEntry
import com.couchlist.app.core.data.local.entity.ExportMediaItem
import com.couchlist.app.core.data.local.entity.LibraryExportData
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.ListGroupEntity
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

    override suspend fun exportToJson(): String = database.withTransaction {
        val mediaItems = mediaItemDao.getAll().map { entity ->
            val runtime = mediaItemDao.getRuntimeMinutes(entity.id)
            entity.toExport(runtime)
        }
        val libraryEntities = libraryItemDao.getAll()
        val libraryItems = libraryEntities.map { entity ->
            val media = mediaItemDao.getById(entity.mediaId)
            entity.toExport(media)
        }
        val listGroups = mediaListDao.getAllGroups().map { it.toExport() }
        val lists = mediaListDao.getAllLists().map { list ->
            val cover = list.coverMediaId?.let { mediaItemDao.getById(it) }
            list.toExport(cover)
        }
        val libraryById = libraryEntities.associateBy { it.id }
        val joins = mediaListDao.getAllJoins()
        val listMemberships = joins.mapNotNull { join ->
            val list = mediaListDao.getListById(join.listId) ?: return@mapNotNull null
            val library = libraryById[join.libraryItemId] ?: return@mapNotNull null
            val media = mediaItemDao.getById(library.mediaId) ?: return@mapNotNull null
            ExportListMembership(
                listId = list.id,
                source = media.source,
                category = media.category.name,
                externalId = media.externalId,
                listName = list.name,
                listType = list.type.name,
                addedAt = join.addedAt,
            )
        }
        val logEntries = logEntryDao.getAll().mapNotNull { entity ->
            val media = mediaItemDao.getById(entity.mediaId) ?: return@mapNotNull null
            entity.toExport(media)
        }

        val export = LibraryExportData(
            mediaItems = mediaItems,
            libraryItems = libraryItems,
            listGroups = listGroups,
            lists = lists,
            listMemberships = listMemberships,
            logEntries = logEntries,
        )
        json.encodeToString(LibraryExportData.serializer(), export)
    }

    override suspend fun importFromJson(jsonString: String) {
        val data = json.decodeFromString(LibraryExportData.serializer(), jsonString)
        validateImportData(data)

        database.withTransaction {
            logEntryDao.deleteAll()
            mediaListDao.deleteAllJoins()
            mediaListDao.deleteAllLists()
            mediaListDao.deleteAllGroups()
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

            // Import library items.
            val libraryIdMap = mutableMapOf<String, Long>()
            val libraryEntities = data.libraryItems.mapNotNull { export ->
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val localMediaId = mediaIdMap[referenceKey(source, category, externalId)]
                    ?: return@mapNotNull null
                export.toEntity(localMediaId)
            }
            libraryItemDao.insertAll(libraryEntities)
            data.libraryItems.forEach { export ->
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val key = referenceKey(export.resolvedSource(), category, export.resolvedExternalId())
                val mediaId = mediaIdMap[key] ?: return@forEach
                libraryItemDao.getByMediaId(mediaId)?.let { libraryIdMap[key] = it.id }
            }

            // Import groups before lists so group foreign keys can be resolved.
            val groupIdMap = mutableMapOf<Long, Long>()
            val groupIds = mediaListDao.insertAllGroups(data.listGroups.map { it.toEntity() })
            data.listGroups.forEachIndexed { index, export ->
                export.id?.let { groupIdMap[it] = groupIds[index] }
            }

            // Import lists and build both v3 ID and legacy name/type maps.
            val listIdMap = mutableMapOf<Long, Long>()
            val legacyListIdMap = mutableMapOf<Pair<String, String>, Long>()
            val listEntities = data.lists.map { export ->
                val coverMediaId = export.coverReferenceKey()?.let(mediaIdMap::get)
                export.toEntity(
                    localGroupId = export.groupId?.let(groupIdMap::get),
                    coverMediaId = coverMediaId,
                )
            }
            val insertedListIds = mediaListDao.insertAllLists(listEntities)
            data.lists.forEachIndexed { index, export ->
                val localId = insertedListIds[index]
                export.id?.let { listIdMap[it] = localId }
                legacyListIdMap[export.name to export.type] = localId
            }

            val now = System.currentTimeMillis()
            val pile = mediaListDao.getPile() ?: MediaListEntity(
                name = DEFAULT_PILE_NAME,
                description = null,
                type = MediaListType.PILE,
                groupId = null,
                coverMediaId = null,
                isPinned = true,
                sortOrder = 0,
                smartFilterJson = null,
                createdAt = now,
                updatedAt = now,
            ).let { list ->
                list.copy(id = mediaListDao.insertListIgnore(list))
            }
            mediaListDao.convertDuplicatePiles(pile.id, now)
            mediaListDao.normalizePile(pile.id, now)

            // Import list memberships.
            val joins = data.listMemberships.mapNotNull { export ->
                val localListId = export.listId?.let(listIdMap::get)
                    ?: legacyListIdMap[export.legacyListKey()]
                    ?: return@mapNotNull null
                val source = export.resolvedSource()
                val category = MediaCategory.valueOf(export.resolvedCategory())
                val externalId = export.resolvedExternalId()
                val key = referenceKey(source, category, externalId)
                val localMediaId = mediaIdMap[key]
                    ?: return@mapNotNull null
                val localLibraryItemId = libraryIdMap[key] ?: libraryItemDao.insertIgnore(
                    LibraryItemEntity(
                        mediaId = localMediaId,
                        status = MediaStatus.BACKLOG,
                        progress = null,
                        personalRating = null,
                        favorite = false,
                        notes = null,
                        addedAt = export.addedAt.takeIf { it > 0L } ?: now,
                        startedAt = null,
                        completedAt = null,
                        updatedAt = export.addedAt.takeIf { it > 0L } ?: now,
                    ),
                ).takeIf { it != -1L }
                    ?: checkNotNull(libraryItemDao.getByMediaId(localMediaId)).id
                libraryIdMap[key] = localLibraryItemId
                MediaListJoinEntity(
                    listId = localListId,
                    libraryItemId = localLibraryItemId,
                    addedAt = export.addedAt.takeIf { it > 0L } ?: now,
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
        require(data.version in 1..3) { "Unsupported export version ${data.version}" }
        val mediaKeys = data.mediaItems.map { item ->
            referenceKey(
                item.resolvedSource(),
                MediaCategory.valueOf(item.resolvedCategory()),
                item.resolvedExternalId(),
            )
        }
        require(mediaKeys.size == mediaKeys.toSet().size) { "Duplicate media reference" }
        val mediaKeySet = mediaKeys.toSet()

        val libraryKeys = data.libraryItems.map { item ->
            MediaStatus.valueOf(item.status)
            referenceKey(
                item.resolvedSource(),
                MediaCategory.valueOf(item.resolvedCategory()),
                item.resolvedExternalId(),
            ).also { require(it in mediaKeySet) { "Unknown library media" } }
        }
        require(libraryKeys.size == libraryKeys.toSet().size) { "Duplicate library item" }

        if (data.version >= 3) {
            require(data.listGroups.all { it.id != null }) { "List group ID is required" }
            require(data.lists.all { it.id != null }) { "List ID is required" }
        }
        val groupIdValues = data.listGroups.mapNotNull { it.id }
        require(groupIdValues.size == groupIdValues.toSet().size) { "Duplicate list group ID" }
        val groupIds = groupIdValues.toSet()
        val listIdValues = data.lists.mapNotNull { it.id }
        require(listIdValues.size == listIdValues.toSet().size) { "Duplicate list ID" }
        val listIds = listIdValues.toSet()
        val legacyListKeys = data.lists.map { it.name to it.type }
        if (data.version < 3) {
            require(legacyListKeys.size == legacyListKeys.toSet().size) {
                "Legacy export has ambiguous list names"
            }
        }
        data.lists.forEach { list ->
            MediaListType.valueOf(list.type)
            require(list.groupId == null || list.groupId in groupIds) { "Unknown list group" }
            val coverParts = listOf(list.coverSource, list.coverCategory, list.coverExternalId)
            require(coverParts.all { it == null } || coverParts.all { it != null }) {
                "Incomplete list cover reference"
            }
            list.coverReferenceKey()?.let { key ->
                require(key in mediaKeySet) { "Unknown list cover media" }
            }
        }
        data.listMemberships.forEach { membership ->
            val mediaKey = referenceKey(
                membership.resolvedSource(),
                MediaCategory.valueOf(membership.resolvedCategory()),
                membership.resolvedExternalId(),
            )
            require(mediaKey in mediaKeySet) { "Unknown list membership media" }
            if (membership.listId != null) {
                require(membership.listId in listIds) { "Unknown list" }
            } else {
                require(data.version < 3) { "List membership ID is required" }
                require(membership.legacyListKey() in legacyListKeys) { "Unknown list" }
            }
        }
        data.logEntries.forEach { entry ->
            val mediaKey = referenceKey(
                entry.resolvedSource(),
                MediaCategory.valueOf(entry.resolvedCategory()),
                entry.resolvedExternalId(),
            )
            require(mediaKey in mediaKeySet) { "Unknown log entry media" }
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

    private fun ListGroupEntity.toExport() = ExportListGroup(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun MediaListEntity.toExport(cover: MediaItemEntity?) = ExportList(
        id = id,
        name = name,
        description = description,
        type = type.name,
        groupId = groupId,
        coverSource = cover?.source,
        coverCategory = cover?.category?.name,
        coverExternalId = cover?.externalId,
        isPinned = isPinned,
        sortOrder = sortOrder,
        smartFilterJson = smartFilterJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ExportList.toEntity(localGroupId: Long?, coverMediaId: Long?) = MediaListEntity(
        name = name,
        description = description,
        type = MediaListType.valueOf(type),
        groupId = localGroupId,
        coverMediaId = coverMediaId,
        isPinned = isPinned,
        sortOrder = sortOrder,
        smartFilterJson = smartFilterJson,
        createdAt = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
        updatedAt = updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
    )

    private fun ExportListGroup.toEntity() = ListGroupEntity(
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
        updatedAt = updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
    )

    private fun ExportList.coverReferenceKey(): String? {
        val source = coverSource ?: return null
        val category = coverCategory?.let(MediaCategory::valueOf) ?: return null
        val externalId = coverExternalId ?: return null
        return referenceKey(source, category, externalId)
    }

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

    private companion object {
        const val DEFAULT_PILE_NAME = "The Pile"
    }
}
