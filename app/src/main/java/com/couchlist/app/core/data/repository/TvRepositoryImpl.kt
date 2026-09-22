package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.TvDao
import com.couchlist.app.core.data.local.entity.EpisodeEntity
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.data.remote.provider.MetadataProviderRegistry
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.TvEpisode
import com.couchlist.app.core.domain.model.TvProgress
import com.couchlist.app.core.domain.model.TvSeason
import com.couchlist.app.core.domain.repository.TvRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TvRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val mediaItemDao: MediaItemDao,
    private val tvDao: TvDao,
    private val providerRegistry: MetadataProviderRegistry,
) : TvRepository {

    override fun observeSeasons(mediaId: Long): Flow<List<TvSeason>> =
        tvDao.observeSeasons(mediaId).map { seasons -> seasons.map { it.toDomain() } }

    override fun observeEpisodes(mediaId: Long, seasonNumber: Int): Flow<List<TvEpisode>> =
        tvDao.observeEpisodes(mediaId, seasonNumber).map { episodes ->
            episodes.map { it.toDomain() }
        }

    override fun observeProgress(mediaId: Long): Flow<TvProgress> =
        tvDao.observeProgress(mediaId).map { row ->
            TvProgress(
                watchedEpisodes = row?.watchedEpisodes ?: 0,
                totalEpisodes = row?.totalEpisodes ?: 0,
            )
        }

    override fun observeNextUnwatched(mediaId: Long): Flow<TvEpisode?> =
        tvDao.observeNextUnwatched(mediaId).map { row -> row?.toDomain() }

    override suspend fun refreshSeason(mediaId: Long, seasonNumber: Int): Result<Unit> {
        val media = mediaItemDao.getById(mediaId)
            ?: return Result.failure(IllegalArgumentException("Unknown media item"))
        if (media.category != MediaCategory.TV) {
            return Result.failure(IllegalArgumentException("Seasons are only available for TV"))
        }
        val reference = MediaReference(media.source, media.category, media.externalId)
        return try {
            val details = providerRegistry.seasonDetails(reference, seasonNumber)
            database.withTransaction {
                val seasonId = tvDao.upsertSeason(
                    SeasonEntity(
                        mediaId = mediaId,
                        seasonNumber = details.season.seasonNumber,
                        name = details.season.name,
                        overview = details.season.overview,
                        artworkUri = details.season.artworkUri,
                        airDate = details.season.airDate,
                        episodeCount = details.season.episodeCount,
                        lastRefreshedAt = System.currentTimeMillis(),
                    ),
                )
                details.episodes.forEach { episode ->
                    tvDao.upsertEpisode(
                        EpisodeEntity(
                            mediaId = mediaId,
                            seasonId = seasonId,
                            seasonNumber = episode.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            title = episode.title,
                            overview = episode.overview,
                            artworkUri = episode.artworkUri,
                            airDate = episode.airDate,
                            runtimeMinutes = episode.runtimeMinutes,
                        ),
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setEpisodeWatched(episodeId: Long, watched: Boolean): Result<Unit> =
        try {
            database.withTransaction {
                val episode = tvDao.getEpisode(episodeId)
                    ?: throw IllegalArgumentException("Unknown episode")
                val action = if (watched) LogAction.EPISODE_WATCHED else LogAction.EPISODE_UNWATCHED
                if (tvDao.getLatestEpisodeAction(episodeId) == action) return@withTransaction

                val now = System.currentTimeMillis()
                tvDao.insertLogEntry(
                    LogEntryEntity(
                        mediaId = episode.mediaId,
                        episodeId = episode.id,
                        action = action,
                        date = now,
                        personalRating = null,
                        notes = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
