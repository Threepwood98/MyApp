package com.couchlist.app.core.domain.model

enum class WatchStatus(val displayName: String) {
    WATCHLIST("Watchlist"),
    WATCHING("Watching"),
    WATCHED("Watched"),
}

val WatchStatus.nextStatus: WatchStatus?
    get() = when (this) {
        WatchStatus.WATCHLIST -> WatchStatus.WATCHING
        WatchStatus.WATCHING -> WatchStatus.WATCHED
        WatchStatus.WATCHED -> null
    }