package com.couchlist.app.core.domain.model

enum class TrackingMode(val displayName: String) {
    JUST_ENJOYING("Just enjoying"),
    QUICK_LOG("Quick log"),
    SIMPLE_COUNTER("Simple counter"),
    CHECKLIST("Checklist"),
    JOURNAL("Journal"),
}

enum class TrackingState {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ABANDONED,
}

enum class TrackingCheckpointKind {
    GROUP,
    ITEM,
}

enum class TrackingCheckpointOrigin {
    USER,
    PROVIDER,
}

data class TrackingSession(
    val id: Long,
    val libraryItemId: Long,
    val mode: TrackingMode,
    val state: TrackingState,
    val isCurrent: Boolean = true,
    val startedAt: Long,
    val endedAt: Long?,
    val legacyProgressFraction: Double?,
    val createdAt: Long,
    val updatedAt: Long,
    val details: TrackingDetails,
) {
    val progress: Double?
        get() = when (mode) {
            TrackingMode.SIMPLE_COUNTER, TrackingMode.CHECKLIST ->
                details.progress ?: legacyProgressFraction
            TrackingMode.JUST_ENJOYING,
            TrackingMode.QUICK_LOG,
            TrackingMode.JOURNAL,
            -> null
        }

    fun toSummary() = TrackingSummary(
        sessionId = id,
        mode = mode,
        state = state,
        progress = progress,
        startedAt = startedAt,
        endedAt = endedAt,
        updatedAt = updatedAt,
    )
}

data class TrackingSummary(
    val sessionId: Long,
    val mode: TrackingMode,
    val state: TrackingState,
    val progress: Double?,
    val startedAt: Long,
    val endedAt: Long?,
    val updatedAt: Long,
)

sealed interface TrackingDetails {
    val progress: Double?

    data object JustEnjoying : TrackingDetails {
        override val progress: Double? = null
    }

    data class QuickLog(
        val entries: List<TrackingQuickLogEntry>,
    ) : TrackingDetails {
        override val progress: Double? = null
    }

    data class SimpleCounter(
        val current: Double,
        val total: Double?,
        val unit: String?,
    ) : TrackingDetails {
        override val progress: Double?
            get() = total?.takeIf { it > 0.0 }?.let { current / it }
    }

    data class Checklist(
        val checkpoints: List<TrackingCheckpoint>,
    ) : TrackingDetails {
        override val progress: Double?
            get() {
                val leaves = checkpoints.filter { it.kind == TrackingCheckpointKind.ITEM }
                return leaves.takeIf { it.isNotEmpty() }
                    ?.let { items -> items.count { it.completedAt != null }.toDouble() / items.size }
            }
    }

    data class Journal(
        val entries: List<TrackingJournalEntry>,
    ) : TrackingDetails {
        override val progress: Double? = null
    }
}

data class TrackingCheckpoint(
    val id: Long,
    val sessionId: Long,
    val stableKey: String,
    val parentId: Long?,
    val kind: TrackingCheckpointKind,
    val origin: TrackingCheckpointOrigin,
    val label: String,
    val sortOrder: Int,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class TrackingQuickLogEntry(
    val id: Long,
    val sessionId: Long,
    val occurredAt: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class TrackingJournalEntry(
    val id: Long,
    val sessionId: Long,
    val title: String,
    val occurredAt: Long,
    val notes: String?,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

val TrackingSummary.status: MediaStatus
    get() = when (state) {
        TrackingState.ACTIVE -> MediaStatus.WATCHING
        TrackingState.PAUSED -> MediaStatus.BACKLOG
        TrackingState.COMPLETED -> MediaStatus.COMPLETED
        TrackingState.ABANDONED -> MediaStatus.ABANDONED
    }

val MediaCategory.defaultTrackingMode: TrackingMode
    get() = when (this) {
        MediaCategory.MOVIE -> TrackingMode.JUST_ENJOYING
        MediaCategory.TV, MediaCategory.ANIME -> TrackingMode.CHECKLIST
        MediaCategory.BOOK, MediaCategory.MANGA, MediaCategory.COMIC -> TrackingMode.SIMPLE_COUNTER
        MediaCategory.VIDEO_GAME,
        MediaCategory.PODCAST,
        MediaCategory.MUSIC,
        MediaCategory.CUSTOM,
        -> TrackingMode.JUST_ENJOYING
    }

val MediaCategory.defaultTrackingUnit: String?
    get() = when (this) {
        MediaCategory.BOOK -> "pages"
        MediaCategory.MANGA -> "chapters"
        MediaCategory.COMIC -> "issues"
        else -> null
    }
