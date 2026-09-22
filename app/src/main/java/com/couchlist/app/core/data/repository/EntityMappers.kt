package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.ListGroupEntity
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaItemWithRuntimeRow
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListSummaryRow
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.data.local.entity.TrackingCheckpointEntity
import com.couchlist.app.core.data.local.entity.TrackingCounterEntity
import com.couchlist.app.core.data.local.entity.TrackingJournalEntryEntity
import com.couchlist.app.core.data.local.entity.TrackingQuickLogEntity
import com.couchlist.app.core.data.local.entity.TrackingSessionEntity
import com.couchlist.app.core.data.local.entity.TrackingSessionWithDetails
import com.couchlist.app.core.data.local.entity.TvEpisodeWithStateRow
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.ListGroup
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaMetadata
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.TvEpisode
import com.couchlist.app.core.domain.model.TvSeason
import com.couchlist.app.core.domain.model.TrackingCheckpoint
import com.couchlist.app.core.domain.model.TrackingDetails
import com.couchlist.app.core.domain.model.TrackingJournalEntry
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingQuickLogEntry
import com.couchlist.app.core.domain.model.TrackingSession
import com.couchlist.app.core.domain.model.TrackingSummary

internal fun MediaItemEntity.toDomain(runtimeMinutes: Int? = null) = MediaItem(
    id = id,
    reference = MediaReference(source, category, externalId),
    title = title,
    originalTitle = originalTitle,
    description = description,
    artworkUri = artworkUri,
    backdropUri = backdropUri,
    releaseDate = releaseDate,
    originalLanguage = originalLanguage,
    externalRating = externalRating,
    externalVoteCount = externalVoteCount,
    genres = genres?.split(", ")?.filter { it.isNotBlank() }.orEmpty(),
    metadata = MediaMetadata.Video(runtimeMinutes),
    lastRefreshedAt = lastRefreshedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun MediaItemWithRuntimeRow.toDomain() = media.toDomain(runtimeMinutes)

internal fun LibraryItemEntity.toDomain() = LibraryItem(
    id = id,
    mediaId = mediaId,
    personalRating = personalRating,
    favorite = favorite,
    notes = notes,
    addedAt = addedAt,
    updatedAt = updatedAt,
)

internal fun LibraryItem.toEntity() = LibraryItemEntity(
    id = id,
    mediaId = mediaId,
    personalRating = personalRating,
    favorite = favorite,
    notes = notes,
    addedAt = addedAt,
    updatedAt = updatedAt,
)

internal fun LibraryMediaRow.toDomain() = LibraryMedia(
    media = media.toDomain(runtimeMinutes),
    library = LibraryItem(
        id = libraryId,
        mediaId = media.id,
        personalRating = personalRating,
        favorite = favorite,
        notes = notes,
        addedAt = addedAt,
        updatedAt = updatedAt,
    ),
    tracking = toTrackingSummary(),
)

private fun LibraryMediaRow.toTrackingSummary(): TrackingSummary? {
    val sessionId = trackingSessionId ?: return null
    val mode = trackingMode ?: return null
    val state = trackingState ?: return null
    val startedAt = trackingStartedAt ?: return null
    val updatedAt = trackingUpdatedAt ?: return null
    val progress = when (mode) {
        TrackingMode.SIMPLE_COUNTER -> {
            val total = trackingCounterTotal
            val current = trackingCounterCurrent
            if (current != null && total != null && total > 0.0) current / total
            else trackingLegacyProgress
        }
        TrackingMode.CHECKLIST -> {
            val total = trackingCheckpointTotal ?: 0
            if (total > 0) (trackingCheckpointCompleted ?: 0).toDouble() / total
            else trackingLegacyProgress
        }
        TrackingMode.JUST_ENJOYING,
        TrackingMode.QUICK_LOG,
        TrackingMode.JOURNAL,
        -> null
    }
    return TrackingSummary(
        sessionId = sessionId,
        mode = mode,
        state = state,
        progress = progress,
        startedAt = startedAt,
        endedAt = trackingEndedAt,
        updatedAt = updatedAt,
    )
}

internal fun TrackingSessionWithDetails.toDomain(): TrackingSession {
    val details = when (session.mode) {
        TrackingMode.JUST_ENJOYING -> TrackingDetails.JustEnjoying
        TrackingMode.QUICK_LOG -> TrackingDetails.QuickLog(
            quickLogs.sortedByDescending { it.occurredAt }.map { it.toDomain() },
        )
        TrackingMode.SIMPLE_COUNTER -> counters.singleOrNull()?.let {
            TrackingDetails.SimpleCounter(it.current, it.total, it.unit)
        } ?: TrackingDetails.SimpleCounter(0.0, null, null)
        TrackingMode.CHECKLIST -> TrackingDetails.Checklist(
            checkpoints.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { it.toDomain() },
        )
        TrackingMode.JOURNAL -> TrackingDetails.Journal(
            journalEntries.sortedByDescending { it.occurredAt }.map { it.toDomain() },
        )
    }
    return TrackingSession(
        id = session.id,
        libraryItemId = session.libraryItemId,
        mode = session.mode,
        state = session.state,
        isCurrent = session.currentSlot != null,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        legacyProgressFraction = session.legacyProgressFraction,
        createdAt = session.createdAt,
        updatedAt = session.updatedAt,
        details = details,
    )
}

internal fun TrackingSession.toEntity(currentSlot: Int?) = TrackingSessionEntity(
    id = id,
    libraryItemId = libraryItemId,
    mode = mode,
    state = state,
    currentSlot = currentSlot,
    startedAt = startedAt,
    endedAt = endedAt,
    legacyProgressFraction = legacyProgressFraction,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingCheckpointEntity.toDomain() = TrackingCheckpoint(
    id = id,
    sessionId = sessionId,
    stableKey = stableKey,
    parentId = parentId,
    kind = kind,
    origin = origin,
    label = label,
    sortOrder = sortOrder,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingCheckpoint.toEntity() = TrackingCheckpointEntity(
    id = id,
    sessionId = sessionId,
    stableKey = stableKey,
    parentId = parentId,
    kind = kind,
    origin = origin,
    label = label,
    sortOrder = sortOrder,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingQuickLogEntity.toDomain() = TrackingQuickLogEntry(
    id = id,
    sessionId = sessionId,
    occurredAt = occurredAt,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingQuickLogEntry.toEntity() = TrackingQuickLogEntity(
    id = id,
    sessionId = sessionId,
    occurredAt = occurredAt,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingJournalEntryEntity.toDomain() = TrackingJournalEntry(
    id = id,
    sessionId = sessionId,
    title = title,
    occurredAt = occurredAt,
    notes = notes,
    imageUri = imageUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingJournalEntry.toEntity() = TrackingJournalEntryEntity(
    id = id,
    sessionId = sessionId,
    title = title,
    occurredAt = occurredAt,
    notes = notes,
    imageUri = imageUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun TrackingDetails.SimpleCounter.toEntity(sessionId: Long, updatedAt: Long) =
    TrackingCounterEntity(
        sessionId = sessionId,
        current = current,
        total = total,
        unit = unit,
        updatedAt = updatedAt,
    )

internal fun MediaListEntity.toDomain() = MediaList(
    id = id,
    name = name,
    description = description,
    type = type,
    groupId = groupId,
    coverMediaId = coverMediaId,
    isPinned = isPinned,
    sortOrder = sortOrder,
    smartFilterJson = smartFilterJson,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun MediaListSummaryRow.toDomain() = MediaListSummary(
    list = list.toDomain(),
    itemCount = itemCount,
    coverArtworkUri = coverArtworkUri,
)

internal fun ListGroupEntity.toDomain() = ListGroup(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun SeasonEntity.toDomain() = TvSeason(
    id = id,
    mediaId = mediaId,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    artworkUri = artworkUri,
    airDate = airDate,
    episodeCount = episodeCount,
    lastRefreshedAt = lastRefreshedAt,
)

internal fun TvEpisodeWithStateRow.toDomain() = TvEpisode(
    id = episode.id,
    mediaId = episode.mediaId,
    seasonId = episode.seasonId,
    seasonNumber = episode.seasonNumber,
    episodeNumber = episode.episodeNumber,
    title = episode.title,
    overview = episode.overview,
    artworkUri = episode.artworkUri,
    airDate = episode.airDate,
    runtimeMinutes = episode.runtimeMinutes,
    isWatched = isWatched,
)
