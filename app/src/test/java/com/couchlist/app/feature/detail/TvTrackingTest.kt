package com.couchlist.app.feature.detail

import com.couchlist.app.core.domain.model.TvProgress
import com.couchlist.app.core.domain.model.TvSeason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvTrackingTest {

    @Test
    fun `season one is selected ahead of specials`() {
        val seasons = listOf(season(0), season(1), season(2))

        assertEquals(1, seasons.defaultSeasonNumber())
    }

    @Test
    fun `first regular season is selected when season one is absent`() {
        val seasons = listOf(season(0), season(3), season(4))

        assertEquals(3, seasons.defaultSeasonNumber())
    }

    @Test
    fun `specials are selected when no regular season exists`() {
        assertEquals(0, listOf(season(0)).defaultSeasonNumber())
    }

    @Test
    fun `progress fraction is null without known episodes`() {
        assertNull(TvProgress(watchedEpisodes = 0, totalEpisodes = 0).fraction)
    }

    @Test
    fun `progress fraction represents watched regular episodes`() {
        assertEquals(0.25, TvProgress(watchedEpisodes = 2, totalEpisodes = 8).fraction!!, 0.0)
    }

    private fun season(number: Int) = TvSeason(
        id = number.toLong(),
        mediaId = 1L,
        seasonNumber = number,
        name = if (number == 0) "Specials" else "Season $number",
        overview = null,
        posterPath = null,
        airDate = null,
        episodeCount = 8,
        lastRefreshedAt = null,
    )
}
