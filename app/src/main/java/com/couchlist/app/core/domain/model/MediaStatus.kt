package com.couchlist.app.core.domain.model

enum class MediaStatus(val displayName: String) {
    BACKLOG("Backlog"),
    WATCHING("Watching"),
    COMPLETED("Completed"),
    ABANDONED("Abandoned"),
}

val MediaStatus.nextStatus: MediaStatus?
    get() = when (this) {
        MediaStatus.BACKLOG -> MediaStatus.WATCHING
        MediaStatus.WATCHING -> MediaStatus.COMPLETED
        MediaStatus.COMPLETED -> null
        MediaStatus.ABANDONED -> null
    }