package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.dao.TrackingDao
import com.couchlist.app.core.data.local.entity.ExportLibraryItem
import com.couchlist.app.core.data.local.entity.ExportList
import com.couchlist.app.core.data.local.entity.ExportListGroup
import com.couchlist.app.core.data.local.entity.ExportListMembership
import com.couchlist.app.core.data.local.entity.ExportLogEntry
import com.couchlist.app.core.data.local.entity.ExportMediaItem
import com.couchlist.app.core.data.local.entity.ExportTrackingCheckpoint
import com.couchlist.app.core.data.local.entity.ExportTrackingCounter
import com.couchlist.app.core.data.local.entity.ExportTrackingJournalEntry
import com.couchlist.app.core.data.local.entity.ExportTrackingQuickLog
import com.couchlist.app.core.data.local.entity.ExportTrackingSession
import com.couchlist.app.core.data.local.entity.LibraryExportData
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.ListGroupEntity
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.data.local.entity.VideoMetadataEntity
import com.couchlist.app.core.data.local.entity.TrackingCheckpointEntity
import com.couchlist.app.core.data.local.entity.TrackingCounterEntity
import com.couchlist.app.core.data.local.entity.TrackingJournalEntryEntity
import com.couchlist.app.core.data.local.entity.TrackingQuickLogEntity
import com.couchlist.app.core.data.local.entity.TrackingSessionEntity
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.TrackingCheckpointKind
import com.couchlist.app.core.domain.model.TrackingCheckpointOrigin
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.domain.model.defaultTrackingMode
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
    private val trackingDao: TrackingDao,
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
        val trackingSessions = trackingDao.getAllSessions().mapNotNull { entity ->
            val library = libraryItemDao.getById(entity.libraryItemId) ?: return@mapNotNull null
            val media = mediaItemDao.getById(library.mediaId) ?: return@mapNotNull null
            entity.toExport(media)
        }
        val trackingCounters = trackingDao.getAllCounters().map { it.toExport() }
        val trackingCheckpoints = trackingDao.getAllCheckpoints().map { it.toExport() }
        val trackingQuickLogs = trackingDao.getAllQuickLogs().map { it.toExport() }
        val trackingJournalEntries = trackingDao.getAllJournalEntries().map { it.toExport() }

        val export = LibraryExportData(
            mediaItems = mediaItems,
            libraryItems = libraryItems,
            listGroups = listGroups,
            lists = lists,
            listMemberships = listMemberships,
            logEntries = logEntries,
            trackingSessions = trackingSessions,
            trackingCounters = trackingCounters,
            trackingCheckpoints = trackingCheckpoints,
            trackingQuickLogs = trackingQuickLogs,
            trackingJournalEntries = trackingJournalEntries,
        )
        json.encodeToString(LibraryExportData.serializer(), export)
    }

    override suspend fun importFromJson(jsonString: String) {
        val data = json.decodeFromString(LibraryExportData.serializer(), jsonString)
        validateImportData(data)

        database.withTransaction {
            trackingDao.deleteAllJournalEntries()
            trackingDao.deleteAllQuickLogs()
            trackingDao.deleteAllCheckpoints()
            trackingDao.deleteAllCounters()
            trackingDao.deleteAllSessions()
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

            if (data.version >= 4) {
                importTracking(data, libraryIdMap)
            } else {
                importLegacyTracking(data, libraryIdMap)
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
                        personalRating = null,
                        favorite = false,
                        notes = null,
                        addedAt = export.addedAt.takeIf { it > 0L } ?: now,
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

    private suspend fun importTracking(
        data: LibraryExportData,
        libraryIdMap: Map<String, Long>,
    ) {
        val sessionIdMap = mutableMapOf<Long, Long>()
        data.trackingSessions.sortedWith(compareBy({ it.createdAt }, { it.id })).forEach { export ->
            val category = MediaCategory.valueOf(export.category)
            val libraryItemId = checkNotNull(
                libraryIdMap[referenceKey(export.source, category, export.externalId)],
            )
            val localId = trackingDao.insertSession(
                TrackingSessionEntity(
                    libraryItemId = libraryItemId,
                    mode = TrackingMode.valueOf(export.mode),
                    state = TrackingState.valueOf(export.state),
                    currentSlot = if (export.isCurrent) 1 else null,
                    startedAt = export.startedAt,
                    endedAt = export.endedAt,
                    legacyProgressFraction = export.legacyProgressFraction,
                    createdAt = export.createdAt,
                    updatedAt = export.updatedAt,
                ),
            )
            sessionIdMap[export.id] = localId
        }

        trackingDao.insertCounters(
            data.trackingCounters.map { export ->
                TrackingCounterEntity(
                    sessionId = checkNotNull(sessionIdMap[export.sessionId]),
                    current = export.current,
                    total = export.total,
                    unit = export.unit,
                    updatedAt = export.updatedAt,
                )
            },
        )

        val checkpointIdMap = mutableMapOf<Long, Long>()
        val remaining = data.trackingCheckpoints.toMutableList()
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { it.parentId == null || it.parentId in checkpointIdMap }
            check(ready.isNotEmpty()) { "Checkpoint hierarchy cannot be imported" }
            ready.forEach { export ->
                val localId = trackingDao.insertCheckpoint(
                    TrackingCheckpointEntity(
                        sessionId = checkNotNull(sessionIdMap[export.sessionId]),
                        stableKey = export.stableKey,
                        parentId = export.parentId?.let { checkNotNull(checkpointIdMap[it]) },
                        kind = TrackingCheckpointKind.valueOf(export.kind),
                        origin = TrackingCheckpointOrigin.valueOf(export.origin),
                        label = export.label,
                        sortOrder = export.sortOrder,
                        completedAt = export.completedAt,
                        createdAt = export.createdAt,
                        updatedAt = export.updatedAt,
                    ),
                )
                checkpointIdMap[export.id] = localId
            }
            remaining.removeAll(ready.toSet())
        }

        trackingDao.insertQuickLogs(
            data.trackingQuickLogs.map { export ->
                TrackingQuickLogEntity(
                    sessionId = checkNotNull(sessionIdMap[export.sessionId]),
                    occurredAt = export.occurredAt,
                    note = export.note,
                    createdAt = export.createdAt,
                    updatedAt = export.updatedAt,
                )
            },
        )
        trackingDao.insertJournalEntries(
            data.trackingJournalEntries.map { export ->
                TrackingJournalEntryEntity(
                    sessionId = checkNotNull(sessionIdMap[export.sessionId]),
                    title = export.title,
                    occurredAt = export.occurredAt,
                    notes = export.notes,
                    imageUri = export.imageUri,
                    createdAt = export.createdAt,
                    updatedAt = export.updatedAt,
                )
            },
        )
    }

    private suspend fun importLegacyTracking(
        data: LibraryExportData,
        libraryIdMap: Map<String, Long>,
    ) {
        val now = System.currentTimeMillis()
        data.libraryItems.forEach { export ->
            val status = MediaStatus.valueOf(checkNotNull(export.status))
            if (status == MediaStatus.BACKLOG && export.progress == null) return@forEach
            val category = MediaCategory.valueOf(export.resolvedCategory())
            val key = referenceKey(export.resolvedSource(), category, export.resolvedExternalId())
            val libraryItemId = libraryIdMap[key] ?: return@forEach
            val addedAt = export.addedAt.takeIf { it > 0L } ?: now
            val updatedAt = export.updatedAt.takeIf { it > 0L } ?: addedAt
            trackingDao.insertSession(
                TrackingSessionEntity(
                    libraryItemId = libraryItemId,
                    mode = category.defaultTrackingMode,
                    state = when (status) {
                        MediaStatus.BACKLOG -> TrackingState.PAUSED
                        MediaStatus.WATCHING -> TrackingState.ACTIVE
                        MediaStatus.COMPLETED -> TrackingState.COMPLETED
                        MediaStatus.ABANDONED -> TrackingState.ABANDONED
                    },
                    currentSlot = 1,
                    startedAt = export.startedAt ?: addedAt,
                    endedAt = when (status) {
                        MediaStatus.COMPLETED -> export.completedAt ?: updatedAt
                        MediaStatus.ABANDONED -> updatedAt
                        MediaStatus.BACKLOG, MediaStatus.WATCHING -> null
                    },
                    legacyProgressFraction = export.progress,
                    createdAt = addedAt,
                    updatedAt = updatedAt,
                ),
            )
        }
    }

    private fun validateImportData(data: LibraryExportData) {
        require(data.version in 1..4) { "Unsupported export version ${data.version}" }
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
            if (data.version < 4) {
                MediaStatus.valueOf(checkNotNull(item.status) { "Legacy library status is required" })
            } else {
                item.status?.let(MediaStatus::valueOf)
            }
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
        if (data.version >= 4) {
            validateTracking(data, libraryKeys.toSet())
        }
    }

    private fun validateTracking(data: LibraryExportData, libraryKeys: Set<String>) {
        val sessionIds = data.trackingSessions.map { it.id }
        require(sessionIds.all { it > 0L }) { "Tracking session IDs must be positive" }
        require(sessionIds.size == sessionIds.toSet().size) { "Duplicate tracking session ID" }
        val sessionById = data.trackingSessions.associateBy { it.id }

        data.trackingSessions.forEach { session ->
            val category = MediaCategory.valueOf(session.category)
            val key = referenceKey(session.source, category, session.externalId)
            require(key in libraryKeys) { "Unknown tracking library item" }
            TrackingMode.valueOf(session.mode)
            val state = TrackingState.valueOf(session.state)
            require(session.startedAt > 0L && session.createdAt > 0L && session.updatedAt > 0L) {
                "Invalid tracking session timestamp"
            }
            require(session.updatedAt >= session.createdAt) { "Tracking session update predates creation" }
            require(session.endedAt == null || session.endedAt >= session.startedAt) {
                "Tracking session ends before it starts"
            }
            require(
                (state == TrackingState.ACTIVE || state == TrackingState.PAUSED) ==
                    (session.endedAt == null),
            ) { "Tracking session state and end timestamp disagree" }
            require(session.legacyProgressFraction?.isFinite() != false) {
                "Invalid legacy tracking progress"
            }
        }
        val currentKeys = data.trackingSessions.filter { it.isCurrent }.map {
            referenceKey(it.source, MediaCategory.valueOf(it.category), it.externalId)
        }
        require(currentKeys.size == currentKeys.toSet().size) { "Multiple current tracking sessions" }

        val counterSessionIds = data.trackingCounters.map { it.sessionId }
        require(counterSessionIds.size == counterSessionIds.toSet().size) { "Duplicate tracking counter" }
        data.trackingCounters.forEach { counter ->
            val session = sessionById[counter.sessionId]
                ?: throw IllegalArgumentException("Unknown tracking counter session")
            require(TrackingMode.valueOf(session.mode) == TrackingMode.SIMPLE_COUNTER) {
                "Counter belongs to a non-counter session"
            }
            require(counter.current.isFinite() && counter.current >= 0.0) { "Invalid counter value" }
            require(counter.total == null || counter.total.isFinite() && counter.total >= 0.0) {
                "Invalid counter total"
            }
            require(counter.updatedAt > 0L) { "Invalid counter timestamp" }
        }

        val checkpointIds = data.trackingCheckpoints.map { it.id }
        require(checkpointIds.all { it > 0L }) { "Tracking checkpoint IDs must be positive" }
        require(checkpointIds.size == checkpointIds.toSet().size) { "Duplicate tracking checkpoint ID" }
        val checkpointById = data.trackingCheckpoints.associateBy { it.id }
        val stableKeys = mutableSetOf<Pair<Long, String>>()
        data.trackingCheckpoints.forEach { checkpoint ->
            val session = sessionById[checkpoint.sessionId]
                ?: throw IllegalArgumentException("Unknown tracking checkpoint session")
            require(TrackingMode.valueOf(session.mode) == TrackingMode.CHECKLIST) {
                "Checkpoint belongs to a non-checklist session"
            }
            require(checkpoint.stableKey.isNotBlank()) { "Checkpoint stable key cannot be blank" }
            require(stableKeys.add(checkpoint.sessionId to checkpoint.stableKey)) {
                "Duplicate checkpoint stable key"
            }
            TrackingCheckpointKind.valueOf(checkpoint.kind)
            TrackingCheckpointOrigin.valueOf(checkpoint.origin)
            require(checkpoint.label.isNotBlank()) { "Checkpoint label cannot be blank" }
            require(checkpoint.createdAt > 0L && checkpoint.updatedAt >= checkpoint.createdAt) {
                "Invalid checkpoint timestamp"
            }
            require(checkpoint.completedAt == null || checkpoint.completedAt >= checkpoint.createdAt) {
                "Checkpoint completion predates creation"
            }
            checkpoint.parentId?.let { parentId ->
                val parent = checkpointById[parentId]
                    ?: throw IllegalArgumentException("Unknown checkpoint parent")
                require(parent.sessionId == checkpoint.sessionId) {
                    "Checkpoint parent belongs to another session"
                }
            }
        }
        checkpointIds.forEach { startId ->
            val visited = mutableSetOf<Long>()
            var currentId: Long? = startId
            while (currentId != null) {
                require(visited.add(currentId)) { "Checkpoint hierarchy contains a cycle" }
                currentId = checkpointById[currentId]?.parentId
            }
        }

        val quickLogIds = data.trackingQuickLogs.map { it.id }
        require(quickLogIds.all { it > 0L } && quickLogIds.size == quickLogIds.toSet().size) {
            "Invalid quick log IDs"
        }
        data.trackingQuickLogs.forEach { entry ->
            val session = sessionById[entry.sessionId]
                ?: throw IllegalArgumentException("Unknown quick log session")
            require(TrackingMode.valueOf(session.mode) == TrackingMode.QUICK_LOG) {
                "Quick log belongs to another tracking mode"
            }
            require(entry.occurredAt > 0L && entry.createdAt > 0L && entry.updatedAt >= entry.createdAt) {
                "Invalid quick log timestamp"
            }
        }

        val journalIds = data.trackingJournalEntries.map { it.id }
        require(journalIds.all { it > 0L } && journalIds.size == journalIds.toSet().size) {
            "Invalid journal entry IDs"
        }
        data.trackingJournalEntries.forEach { entry ->
            val session = sessionById[entry.sessionId]
                ?: throw IllegalArgumentException("Unknown journal session")
            require(TrackingMode.valueOf(session.mode) == TrackingMode.JOURNAL) {
                "Journal entry belongs to another tracking mode"
            }
            require(entry.title.isNotBlank()) { "Journal title cannot be blank" }
            require(entry.occurredAt > 0L && entry.createdAt > 0L && entry.updatedAt >= entry.createdAt) {
                "Invalid journal timestamp"
            }
        }
    }

    // TODO(provider-delegation): Delegate path normalization to the provider registry when
    //  a second metadata provider is added, so each provider owns its image URL construction.
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
        personalRating = personalRating,
        favorite = favorite,
        notes = notes,
        addedAt = addedAt,
        updatedAt = updatedAt,
    )

    private fun ExportLibraryItem.toEntity(localMediaId: Long) = LibraryItemEntity(
        mediaId = localMediaId,
        personalRating = personalRating,
        favorite = favorite,
        notes = notes,
        addedAt = addedAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
        updatedAt = updatedAt.takeIf { it > 0L } ?: addedAt.takeIf { it > 0L }
            ?: System.currentTimeMillis(),
    )

    private fun TrackingSessionEntity.toExport(media: MediaItemEntity) = ExportTrackingSession(
        id = id,
        source = media.source,
        category = media.category.name,
        externalId = media.externalId,
        mode = mode.name,
        state = state.name,
        isCurrent = currentSlot != null,
        startedAt = startedAt,
        endedAt = endedAt,
        legacyProgressFraction = legacyProgressFraction,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TrackingCounterEntity.toExport() = ExportTrackingCounter(
        sessionId = sessionId,
        current = current,
        total = total,
        unit = unit,
        updatedAt = updatedAt,
    )

    private fun TrackingCheckpointEntity.toExport() = ExportTrackingCheckpoint(
        id = id,
        sessionId = sessionId,
        stableKey = stableKey,
        parentId = parentId,
        kind = kind.name,
        origin = origin.name,
        label = label,
        sortOrder = sortOrder,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TrackingQuickLogEntity.toExport() = ExportTrackingQuickLog(
        id = id,
        sessionId = sessionId,
        occurredAt = occurredAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TrackingJournalEntryEntity.toExport() = ExportTrackingJournalEntry(
        id = id,
        sessionId = sessionId,
        title = title,
        occurredAt = occurredAt,
        notes = notes,
        imageUri = imageUri,
        createdAt = createdAt,
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
