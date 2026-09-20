package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaItem

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
