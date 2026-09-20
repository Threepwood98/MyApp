package com.couchlist.app.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailDto(
    val id: Long = 0,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    val runtime: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Long = 0,
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class TvDetailDto(
    val id: Long = 0,
    val name: String = "",
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Long = 0,
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class GenreDto(
    val id: Long = 0,
    val name: String = "",
)
