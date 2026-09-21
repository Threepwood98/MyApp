package com.couchlist.app.core.data.local.entity

import kotlinx.serialization.Serializable

/**
 * Complete library export format. Serialized as JSON for file-based
 * import/export. Uses provider-neutral references (source + category +
 * external_id) for cross-referencing so the file is portable across
 * installations and metadata providers.
 */
@Serializable
data class LibraryExportData(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val mediaItems: List<ExportMediaItem>,
    val libraryItems: List<ExportLibraryItem>,
    val lists: List<ExportList>,
    val listMemberships: List<ExportListMembership>,
    val logEntries: List<ExportLogEntry>,
)

@Serializable
data class ExportMediaItem(
    // v2 fields (provider-neutral)
    val source: String? = null,
    val category: String? = null,
    val externalId: String? = null,
    // v1 legacy fields (kept for backward-compatible import)
    val mediaType: String? = null,
    val tmdbId: Long? = null,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val description: String? = null,
    val posterPath: String? = null,
    val artworkUri: String? = null,
    val backdropPath: String? = null,
    val backdropUri: String? = null,
    val releaseDate: String? = null,
    val originalLanguage: String? = null,
    val runtimeMinutes: Int? = null,
    val externalRating: Double = 0.0,
    val externalVoteCount: Long = 0L,
    val genres: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun resolvedSource(): String = source ?: "tmdb"

    fun resolvedCategory(): String = category ?: mediaType ?: "MOVIE"

    fun resolvedExternalId(): String =
        externalId ?: (tmdbId?.toString() ?: throw IllegalArgumentException("No external ID"))
}

@Serializable
data class ExportLibraryItem(
    // v2 fields
    val source: String? = null,
    val category: String? = null,
    val externalId: String? = null,
    // v1 legacy fields
    val mediaType: String? = null,
    val tmdbId: Long? = null,
    val status: String,
    val progress: Double? = null,
    val personalRating: Int? = null,
    val favorite: Boolean = false,
    val notes: String? = null,
    val addedAt: Long = 0L,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long = 0L,
) {
    fun resolvedSource(): String = source ?: "tmdb"

    fun resolvedCategory(): String = category ?: mediaType ?: "MOVIE"

    fun resolvedExternalId(): String =
        externalId ?: (tmdbId?.toString() ?: throw IllegalArgumentException("No external ID"))
}

@Serializable
data class ExportList(
    val name: String,
    val description: String? = null,
    val type: String,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val smartFilterJson: String? = null,
)

@Serializable
data class ExportListMembership(
    // v2 fields
    val source: String? = null,
    val category: String? = null,
    val externalId: String? = null,
    // v1 legacy fields
    val mediaType: String? = null,
    val tmdbId: Long? = null,
    val listName: String,
    val listType: String,
) {
    fun resolvedSource(): String = source ?: "tmdb"

    fun resolvedCategory(): String = category ?: mediaType ?: "MOVIE"

    fun resolvedExternalId(): String =
        externalId ?: (tmdbId?.toString() ?: throw IllegalArgumentException("No external ID"))
}

@Serializable
data class ExportLogEntry(
    // v2 fields
    val source: String? = null,
    val category: String? = null,
    val externalId: String? = null,
    // v1 legacy fields
    val mediaType: String? = null,
    val tmdbId: Long? = null,
    val action: String,
    val date: Long,
    val personalRating: Int? = null,
    val notes: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun resolvedSource(): String = source ?: "tmdb"

    fun resolvedCategory(): String = category ?: mediaType ?: "MOVIE"

    fun resolvedExternalId(): String =
        externalId ?: (tmdbId?.toString() ?: throw IllegalArgumentException("No external ID"))
}
