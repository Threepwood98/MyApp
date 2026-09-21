package com.couchlist.app.core.domain.model

/** Common provider-neutral metadata with category-specific data kept in [metadata]. */
data class MediaItem(
    val id: Long,
    val reference: MediaReference,
    val title: String,
    val originalTitle: String?,
    val description: String?,
    val artworkUri: String?,
    val backdropUri: String?,
    val releaseDate: String?,
    val originalLanguage: String?,
    val externalRating: Double,
    val externalVoteCount: Long,
    val genres: List<String>,
    val metadata: MediaMetadata,
    val lastRefreshedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val category: MediaCategory
        get() = reference.category

    val runtimeMinutes: Int?
        get() = (metadata as? MediaMetadata.Video)?.runtimeMinutes
}
