package com.couchlist.app.core.data.repository

import androidx.room.withTransaction
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.TvDao
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.data.local.entity.VideoMetadataEntity
import com.couchlist.app.core.data.remote.provider.MetadataProviderRegistry
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaMetadata
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.CatalogRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogRepositoryImpl @Inject constructor(
    private val database: CouchlistDatabase,
    private val mediaItemDao: MediaItemDao,
    private val tvDao: TvDao,
    private val providerRegistry: MetadataProviderRegistry,
) : CatalogRepository {

    override val supportedCategories = providerRegistry.supportedCategories

    override suspend fun search(query: String): Result<List<MediaSearchResult>> = try {
        Result.success(providerRegistry.search(query))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeMedia(mediaId: Long): Flow<MediaItem?> =
        mediaItemDao.observeById(mediaId).map { it?.toDomain() }

    override suspend fun getOrCreate(searchResult: MediaSearchResult): MediaItem {
        val reference = searchResult.reference

        mediaItemDao.getByReference(reference.source, reference.category, reference.externalId)
            ?.let { return it.toDomain() }

        val now = System.currentTimeMillis()
        val rowId = mediaItemDao.insertIgnore(
            MediaItemEntity(
                source = reference.source,
                category = reference.category,
                externalId = reference.externalId,
                title = searchResult.title,
                originalTitle = null,
                description = searchResult.description,
                artworkUri = searchResult.artworkUri,
                backdropUri = null,
                releaseDate = searchResult.releaseYear?.toString(),
                originalLanguage = null,
                genres = null,
                lastRefreshedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val entity = if (rowId == -1L) {
            mediaItemDao.getByReference(reference.source, reference.category, reference.externalId)
        } else {
            mediaItemDao.getById(rowId)
        }
        return checkNotNull(entity) { "Catalog item was not persisted" }.toDomain()
    }

    override suspend fun refresh(mediaId: Long): Result<List<WatchProvider>> {
        val cached = mediaItemDao.getById(mediaId)
            ?: return Result.failure(IllegalArgumentException("Unknown media item"))
        val reference = MediaReference(
            source = cached.source,
            category = cached.category,
            externalId = cached.externalId,
        )
        return try {
            val detail = providerRegistry.details(reference)
            val now = System.currentTimeMillis()
            val runtime = (detail.metadata as? MediaMetadata.Video)?.runtimeMinutes
            database.withTransaction {
                mediaItemDao.updateMetadata(
                    id = cached.id,
                    title = detail.title,
                    originalTitle = detail.originalTitle,
                    description = detail.description,
                    artworkUri = detail.artworkUri,
                    backdropUri = detail.backdropUri,
                    releaseDate = detail.releaseDate,
                    originalLanguage = detail.originalLanguage,
                    externalRating = detail.voteAverage,
                    externalVoteCount = detail.voteCount,
                    genres = detail.genres.joinToString(),
                    refreshedAt = now,
                    updatedAt = now,
                )
                if (runtime != null) {
                    mediaItemDao.upsertVideoMetadata(
                        VideoMetadataEntity(mediaId = cached.id, runtimeMinutes = runtime),
                    )
                }
                detail.seasons.forEach { season ->
                    tvDao.upsertSeason(
                        SeasonEntity(
                            mediaId = cached.id,
                            seasonNumber = season.seasonNumber,
                            name = season.name,
                            overview = season.overview,
                            artworkUri = season.artworkUri,
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
    }
}
