package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.remote.TmdbApi
import com.couchlist.app.core.data.remote.dto.CountryProvidersDto
import com.couchlist.app.core.data.remote.dto.MediaProviderDto
import com.couchlist.app.core.data.remote.dto.MediaResultDto
import com.couchlist.app.core.data.remote.dto.MovieDetailDto
import com.couchlist.app.core.data.remote.dto.SeasonSummaryDto
import com.couchlist.app.core.data.remote.dto.TvDetailDto
import com.couchlist.app.core.data.remote.dto.TvSeasonDetailDto
import com.couchlist.app.core.domain.model.EpisodeMetadata
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.ProviderCategory
import com.couchlist.app.core.domain.model.SeasonDetails
import com.couchlist.app.core.domain.model.SeasonMetadata
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.MediaRepository
import com.couchlist.app.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@Singleton
class TmdbMediaRepository @Inject constructor(
    private val api: TmdbApi,
    private val settingsRepository: SettingsRepository,
) : MediaRepository {

    override suspend fun searchMulti(query: String): Result<List<MediaSearchResult>> =
        try {
            Result.success(api.searchMulti(query).results.mapNotNull { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun details(id: Long, mediaType: MediaType): Result<MediaDetail> =
        try {
            val providers = api.watchProviders(id, mediaType)
            val detail = when (mediaType) {
                MediaType.MOVIE -> api.movie(id).toDomain(mediaType, providers)
                MediaType.TV -> api.tv(id).toDomain(mediaType, providers)
            }
            Result.success(detail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun seasonDetails(tvId: Long, seasonNumber: Int): Result<SeasonDetails> =
        try {
            Result.success(api.tvSeason(tvId, seasonNumber).toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun TmdbApi.watchProviders(id: Long, mediaType: MediaType): List<WatchProvider> {
        val dto = when (mediaType) {
            MediaType.MOVIE -> movieWatchProviders(id)
            MediaType.TV -> tvWatchProviders(id)
        }
        val bundle = dto.results[settingsRepository.settings.first().providerRegion]
            ?: dto.results["US"]
            ?: return emptyList()
        return bundle.toDomain()
    }

    private fun MediaResultDto.toDomain(): MediaSearchResult? {
        val type = when (mediaType) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> return null
        }
        return MediaSearchResult(
            id = id,
            mediaType = type,
            title = title ?: name.orEmpty(),
            posterPath = posterPath,
            releaseYear = (releaseDate ?: firstAirDate).toYear(),
            overview = overview,
            voteAverage = voteAverage,
        )
    }

    private fun MovieDetailDto.toDomain(
        mediaType: MediaType,
        providers: List<WatchProvider>,
    ) = MediaDetail(
        id = id,
        mediaType = mediaType,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        releaseDate = releaseDate,
        originalLanguage = originalLanguage,
        runtimeMinutes = runtime,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = genres.map { it.name },
        providers = providers,
    )

    private fun TvSeasonDetailDto.toDomain() = SeasonDetails(
        season = SeasonMetadata(
            seasonNumber = seasonNumber,
            name = name,
            overview = overview,
            posterPath = posterPath,
            airDate = airDate,
            episodeCount = episodes.size,
        ),
        episodes = episodes.map { episode ->
            EpisodeMetadata(
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                title = episode.name,
                overview = episode.overview,
                stillPath = episode.stillPath,
                airDate = episode.airDate,
                runtimeMinutes = episode.runtime,
            )
        },
    )

    private fun SeasonSummaryDto.toDomain() = SeasonMetadata(
        seasonNumber = seasonNumber,
        name = name,
        overview = overview,
        posterPath = posterPath,
        airDate = airDate,
        episodeCount = episodeCount,
    )

    private fun TvDetailDto.toDomain(
        mediaType: MediaType,
        providers: List<WatchProvider>,
    ) = MediaDetail(
        id = id,
        mediaType = mediaType,
        title = name,
        originalTitle = originalName,
        overview = overview,
        releaseDate = firstAirDate,
        originalLanguage = originalLanguage,
        runtimeMinutes = episodeRunTime.firstOrNull(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = genres.map { it.name },
        providers = providers,
        seasons = seasons.map { it.toDomain() },
    )

    private fun String?.toYear(): Int? =
        this?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()

    private fun CountryProvidersDto.toDomain(): List<WatchProvider> = buildList {
        flatrate.orEmpty().forEach { add(it.toDomain(ProviderCategory.FLATRATE)) }
        rent.orEmpty().forEach { add(it.toDomain(ProviderCategory.RENT)) }
        buy.orEmpty().forEach { add(it.toDomain(ProviderCategory.BUY)) }
    }

    private fun MediaProviderDto.toDomain(category: ProviderCategory) = WatchProvider(
        providerId = providerId,
        name = providerName,
        logoPath = logoPath,
        category = category,
    )
}
