package com.couchlist.app.core.domain.model

data class SeasonMetadata(
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val artworkUri: String?,
    val airDate: String?,
    val episodeCount: Int,
)

data class EpisodeMetadata(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String?,
    val artworkUri: String?,
    val airDate: String?,
    val runtimeMinutes: Int?,
)

data class SeasonDetails(
    val season: SeasonMetadata,
    val episodes: List<EpisodeMetadata>,
)

data class TvSeason(
    val id: Long,
    val mediaId: Long,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val artworkUri: String?,
    val airDate: String?,
    val episodeCount: Int,
    val lastRefreshedAt: Long?,
)

data class TvEpisode(
    val id: Long,
    val mediaId: Long,
    val seasonId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String?,
    val artworkUri: String?,
    val airDate: String?,
    val runtimeMinutes: Int?,
    val isWatched: Boolean,
)

data class TvProgress(
    val watchedEpisodes: Int,
    val totalEpisodes: Int,
) {
    val fraction: Double?
        get() = totalEpisodes.takeIf { it > 0 }?.let { watchedEpisodes.toDouble() / it }
}
