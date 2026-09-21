package com.couchlist.app.core.domain.model

data class MediaDetail(
    val reference: MediaReference,
    val title: String,
    val originalTitle: String?,
    val description: String?,
    val releaseDate: String?,
    val originalLanguage: String?,
    val artworkUri: String?,
    val backdropUri: String?,
    val voteAverage: Double,
    val voteCount: Long,
    val genres: List<String>,
    val providers: List<WatchProvider>,
    val metadata: MediaMetadata,
) {
    val category: MediaCategory
        get() = reference.category

    val runtimeMinutes: Int?
        get() = (metadata as? MediaMetadata.Video)?.runtimeMinutes

    val seasons: List<SeasonMetadata>
        get() = (metadata as? MediaMetadata.Video)?.seasons.orEmpty()

    val releaseYear: Int?
        get() = releaseDate?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()
}
