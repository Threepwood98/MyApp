package com.couchlist.app.core.domain.model

data class MediaSearchResult(
    val reference: MediaReference,
    val title: String,
    val artworkUri: String?,
    val releaseYear: Int?,
    val description: String?,
    val voteAverage: Double = 0.0,
) {
    val category: MediaCategory
        get() = reference.category
}
