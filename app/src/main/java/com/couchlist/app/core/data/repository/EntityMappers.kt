package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.ListGroupEntity
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaItemWithRuntimeRow
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListSummaryRow
import com.couchlist.app.core.data.local.entity.SeasonEntity
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
    status = status,
    progress = progress,
    personalRating = personalRating,
    favorite = favorite,
    notes = notes,
    addedAt = addedAt,
    startedAt = startedAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
)

internal fun LibraryItem.toEntity() = LibraryItemEntity(
    id = id,
    mediaId = mediaId,
    status = status,
    progress = progress,
    personalRating = personalRating,
    favorite = favorite,
    notes = notes,
    addedAt = addedAt,
    startedAt = startedAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
)

internal fun LibraryMediaRow.toDomain() = LibraryMedia(
    media = media.toDomain(runtimeMinutes),
    library = LibraryItem(
        id = libraryId,
        mediaId = media.id,
        status = status,
        progress = progress,
        personalRating = personalRating,
        favorite = favorite,
        notes = notes,
        addedAt = addedAt,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt,
    ),
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
