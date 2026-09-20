package com.couchlist.app.core.domain.model

data class MediaDetail(
    val id: Long,
    val mediaType: MediaType,
    val title: String,
    val overview: String?,
    val releaseYear: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val providers: List<WatchProvider>,
)