package com.couchlist.app.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonSummaryDto(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0,
)

@Serializable
data class TvSeasonDetailDto(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
)

@Serializable
data class EpisodeDto(
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
)
