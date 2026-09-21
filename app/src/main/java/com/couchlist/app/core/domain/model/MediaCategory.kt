package com.couchlist.app.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaCategory {
    MOVIE,
    TV,
    ANIME,
    BOOK,
    MANGA,
    COMIC,
    VIDEO_GAME,
    PODCAST,
    MUSIC,
    CUSTOM,
}
