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
    val version: Int = 4,
    val exportedAt: Long = System.currentTimeMillis(),
    val mediaItems: List<ExportMediaItem>,
    val libraryItems: List<ExportLibraryItem>,
    val listGroups: List<ExportListGroup> = emptyList(),
    val lists: List<ExportList>,
    val listMemberships: List<ExportListMembership>,
    val logEntries: List<ExportLogEntry>,
    val trackingSessions: List<ExportTrackingSession> = emptyList(),
    val trackingCounters: List<ExportTrackingCounter> = emptyList(),
    val trackingCheckpoints: List<ExportTrackingCheckpoint> = emptyList(),
    val trackingQuickLogs: List<ExportTrackingQuickLog> = emptyList(),
    val trackingJournalEntries: List<ExportTrackingJournalEntry> = emptyList(),
)

@Serializable
data class ExportListGroup(
    val id: Long? = null,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
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
    val status: String? = null,
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
data class ExportTrackingSession(
    val id: Long,
    val source: String,
    val category: String,
    val externalId: String,
    val mode: String,
    val state: String,
    val isCurrent: Boolean,
    val startedAt: Long,
    val endedAt: Long? = null,
    val legacyProgressFraction: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ExportTrackingCounter(
    val sessionId: Long,
    val current: Double,
    val total: Double? = null,
    val unit: String? = null,
    val updatedAt: Long,
)

@Serializable
data class ExportTrackingCheckpoint(
    val id: Long,
    val sessionId: Long,
    val stableKey: String,
    val parentId: Long? = null,
    val kind: String,
    val origin: String,
    val label: String,
    val sortOrder: Int,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ExportTrackingQuickLog(
    val id: Long,
    val sessionId: Long,
    val occurredAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ExportTrackingJournalEntry(
    val id: Long,
    val sessionId: Long,
    val title: String,
    val occurredAt: Long,
    val notes: String? = null,
    val imageUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ExportList(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val type: String,
    val groupId: Long? = null,
    val coverSource: String? = null,
    val coverCategory: String? = null,
    val coverExternalId: String? = null,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val smartFilterJson: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class ExportListMembership(
    val listId: Long? = null,
    // v2 fields
    val source: String? = null,
    val category: String? = null,
    val externalId: String? = null,
    // v1 legacy fields
    val mediaType: String? = null,
    val tmdbId: Long? = null,
    val listName: String? = null,
    val listType: String? = null,
    val addedAt: Long = 0L,
) {
    fun resolvedSource(): String = source ?: "tmdb"

    fun resolvedCategory(): String = category ?: mediaType ?: "MOVIE"

    fun resolvedExternalId(): String =
        externalId ?: (tmdbId?.toString() ?: throw IllegalArgumentException("No external ID"))

    fun legacyListKey(): Pair<String, String> =
        checkNotNull(listName) { "No list name" } to
            checkNotNull(listType) { "No list type" }
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
