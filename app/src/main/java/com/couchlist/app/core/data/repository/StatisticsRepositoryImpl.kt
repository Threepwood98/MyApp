package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.domain.model.GenreCount
import com.couchlist.app.core.domain.model.MonthStats
import com.couchlist.app.core.domain.model.Statistics
import com.couchlist.app.core.domain.repository.StatisticsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val logEntryDao: LogEntryDao,
    private val mediaItemDao: MediaItemDao,
) : StatisticsRepository {

    override fun observeStatistics(): Flow<Statistics> {
        val movieCountFlow = logEntryDao.observeMovieCount()
        val episodeCountFlow = logEntryDao.observeEpisodeCount()
        val uniqueTitlesFlow = logEntryDao.observeUniqueTitleCount()
        val avgRatingFlow = logEntryDao.observeAverageRating()
        val ratingDistFlow = logEntryDao.observeRatingDistribution()
        val monthlyFlow = logEntryDao.observeMonthlyActivity(sixMonthsAgo())

        return combine(
            movieCountFlow,
            episodeCountFlow,
            uniqueTitlesFlow,
            avgRatingFlow,
            ratingDistFlow,
            monthlyFlow,
        ) { results: Array<Any?> ->
            val movieCount = results[0] as Int
            val episodeCount = results[1] as Int
            val uniqueTitles = results[2] as Int
            val avgRating = results[3] as? Double
            @Suppress("UNCHECKED_CAST")
            val ratingDist = results[4] as List<com.couchlist.app.core.data.local.entity.RatingCount>
            @Suppress("UNCHECKED_CAST")
            val monthlyActivity = results[5] as List<com.couchlist.app.core.data.local.entity.MonthCount>

            val genreStrings = mediaItemDao.getWatchedGenres()
            val topGenres = computeTopGenres(genreStrings)

            Statistics(
                moviesWatched = movieCount,
                episodesWatched = episodeCount,
                uniqueTitles = uniqueTitles,
                averageRating = avgRating,
                ratingDistribution = ratingDist.associate { it.rating to it.count },
                monthlyActivity = monthlyActivity.map { MonthStats(it.month, it.count) },
                topGenres = topGenres,
            )
        }
    }

    private fun computeTopGenres(genreStrings: List<String>): List<GenreCount> {
        val counts = mutableMapOf<String, Int>()
        genreStrings.forEach { genresCsv ->
            genresCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { genre ->
                counts[genre] = (counts[genre] ?: 0) + 1
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { GenreCount(it.key, it.value) }
    }

    private fun sixMonthsAgo(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -6)
        return cal.timeInMillis
    }
}
