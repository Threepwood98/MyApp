package com.couchlist.app.core.domain.model

/**
 * Pure catalog/metadata for a movie or TV show, sourced from TMDB and cached locally.
 * Contains no user-specific data; see [LibraryItem].
 */
data class MediaItem(
    val id: Long,
    val mediaType: MediaType,
    val tmdbId: Long,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val originalLanguage: String?,
    val runtimeMinutes: Int?,
    val externalRating: Double,
    val externalVoteCount: Long,
    val genres: List<String>,
    val lastRefreshedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
