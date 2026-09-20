package com.couchlist.app.core.domain.model

data class MediaSearchResult(
    val id: Long,
    val mediaType: MediaType,
    val title: String,
    val posterPath: String?,
    val releaseYear: Int?,
    val overview: String?,
)