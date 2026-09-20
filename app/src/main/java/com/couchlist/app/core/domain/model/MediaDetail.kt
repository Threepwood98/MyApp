package com.couchlist.app.core.domain.model

data class MediaDetail(
    val id: Long,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val releaseDate: String?,
    val originalLanguage: String?,
    val runtimeMinutes: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Long,
    val genres: List<String>,
    val providers: List<WatchProvider>,
    val seasons: List<SeasonMetadata> = emptyList(),
) {
    val releaseYear: Int?
        get() = releaseDate?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()
}
