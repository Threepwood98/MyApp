package com.couchlist.app.core.domain.model

data class Statistics(
    val moviesWatched: Int,
    val episodesWatched: Int,
    val uniqueTitles: Int,
    val averageRating: Double?,
    val ratingDistribution: Map<Int, Int>,
    val monthlyActivity: List<MonthStats>,
    val topGenres: List<GenreCount>,
)

data class MonthStats(
    val month: String,
    val count: Int,
)

data class GenreCount(
    val genre: String,
    val count: Int,
)
