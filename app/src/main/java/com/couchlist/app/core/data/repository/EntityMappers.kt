package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListSummaryRow
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.data.local.entity.TvEpisodeWithStateRow
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.TvEpisode
import com.couchlist.app.core.domain.model.TvSeason

internal fun MediaItemEntity.toDomain() = MediaItem(
    id = id,
    mediaType = mediaType,
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
    genres = genres?.split(", ")?.filter { it.isNotBlank() }.orEmpty(),
    lastRefreshedAt = lastRefreshedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

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
    media = media.toDomain(),
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
)

internal fun SeasonEntity.toDomain() = TvSeason(
    id = id,
    mediaId = mediaId,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
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
    stillPath = episode.stillPath,
    airDate = episode.airDate,
    runtimeMinutes = episode.runtimeMinutes,
    isWatched = isWatched,
)
