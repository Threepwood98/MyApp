package com.couchlist.app.core.data.local.entity

import kotlinx.serialization.Serializable

/**
 * Complete library export format. Serialized as JSON for file-based
 * import/export. Uses TMDB IDs (not Room primary keys) for cross-referencing
 * so the file is portable across installations.
 */
@Serializable
data class LibraryExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val mediaItems: List<ExportMediaItem>,
    val libraryItems: List<ExportLibraryItem>,
    val lists: List<ExportList>,
    val listMemberships: List<ExportListMembership>,
    val logEntries: List<ExportLogEntry>,
)

@Serializable
data class ExportMediaItem(
    val mediaType: String,
    val tmdbId: Long,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val originalLanguage: String? = null,
    val runtimeMinutes: Int? = null,
    val externalRating: Double = 0.0,
    val externalVoteCount: Long = 0L,
    val genres: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class ExportLibraryItem(
    val mediaType: String,
    val tmdbId: Long,
    val status: String,
    val progress: Double? = null,
    val personalRating: Int? = null,
    val favorite: Boolean = false,
    val notes: String? = null,
    val addedAt: Long = 0L,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long = 0L,
)

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
    val listName: String,
    val listType: String,
    val mediaType: String,
    val tmdbId: Long,
)

@Serializable
data class ExportLogEntry(
    val mediaType: String,
    val tmdbId: Long,
    val action: String,
    val date: Long,
    val personalRating: Int? = null,
    val notes: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
