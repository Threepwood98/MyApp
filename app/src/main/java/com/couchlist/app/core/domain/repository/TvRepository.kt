package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.TvEpisode
import com.couchlist.app.core.domain.model.TvProgress
import com.couchlist.app.core.domain.model.TvSeason
import kotlinx.coroutines.flow.Flow

interface TvRepository {
    fun observeSeasons(mediaId: Long): Flow<List<TvSeason>>

    fun observeEpisodes(mediaId: Long, seasonNumber: Int): Flow<List<TvEpisode>>

    fun observeProgress(mediaId: Long): Flow<TvProgress>

    suspend fun refreshSeason(mediaId: Long, seasonNumber: Int): Result<Unit>

    suspend fun setEpisodeWatched(episodeId: Long, watched: Boolean): Result<Unit>

    fun observeNextUnwatched(mediaId: Long): Flow<TvEpisode?>
}
