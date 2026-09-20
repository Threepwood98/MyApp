package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.TvDao
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.CatalogRepository
import com.couchlist.app.core.domain.repository.MediaRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val mediaItemDao: MediaItemDao,
    private val tvDao: TvDao,
    private val mediaRepository: MediaRepository,
) : CatalogRepository {

    override fun observeMedia(mediaId: Long): Flow<MediaItem?> =
        mediaItemDao.observeById(mediaId).map { it?.toDomain() }

    override suspend fun getOrCreate(searchResult: MediaSearchResult): MediaItem {
        mediaItemDao.getByTmdb(searchResult.mediaType, searchResult.id)?.let { return it.toDomain() }

        val now = System.currentTimeMillis()
        val rowId = mediaItemDao.insertIgnore(
            MediaItemEntity(
                mediaType = searchResult.mediaType,
                tmdbId = searchResult.id,
                title = searchResult.title,
                originalTitle = null,
                overview = searchResult.overview,
                posterPath = searchResult.posterPath,
                backdropPath = null,
                releaseDate = searchResult.releaseYear?.toString(),
                originalLanguage = null,
                runtimeMinutes = null,
                genres = null,
                lastRefreshedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val entity = if (rowId == -1L) {
            mediaItemDao.getByTmdb(searchResult.mediaType, searchResult.id)
        } else {
            mediaItemDao.getById(rowId)
        }
        return checkNotNull(entity) { "Catalog item was not persisted" }.toDomain()
    }

    override suspend fun refresh(mediaId: Long): Result<List<WatchProvider>> {
        val cached = mediaItemDao.getById(mediaId)
            ?: return Result.failure(IllegalArgumentException("Unknown media item"))
        return mediaRepository.details(cached.tmdbId, cached.mediaType).fold(
            onSuccess = { detail ->
                try {
                    val now = System.currentTimeMillis()
                    database.withTransaction {
                        mediaItemDao.updateMetadata(
                            id = cached.id,
                            title = detail.title,
                            originalTitle = detail.originalTitle,
                            overview = detail.overview,
                            posterPath = detail.posterPath,
                            backdropPath = detail.backdropPath,
                            releaseDate = detail.releaseDate,
                            originalLanguage = detail.originalLanguage,
                            runtimeMinutes = detail.runtimeMinutes,
                            externalRating = detail.voteAverage,
                            externalVoteCount = detail.voteCount,
                            genres = detail.genres.joinToString(),
                            refreshedAt = now,
                            updatedAt = now,
                        )
                        detail.seasons.forEach { season ->
                            tvDao.upsertSeason(
                                SeasonEntity(
                                    mediaId = cached.id,
                                    seasonNumber = season.seasonNumber,
                                    name = season.name,
                                    overview = season.overview,
                                    posterPath = season.posterPath,
                                    airDate = season.airDate,
                                    episodeCount = season.episodeCount,
                                    lastRefreshedAt = now,
                                ),
                            )
                        }
                    }
                    Result.success(detail.providers)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { Result.failure(it) },
        )
    }
}
