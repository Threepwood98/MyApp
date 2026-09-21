package com.couchlist.app.core.data.remote.provider

import com.couchlist.app.core.data.remote.TmdbApi
import com.couchlist.app.core.data.remote.dto.CountryProvidersDto
import com.couchlist.app.core.data.remote.dto.MediaProviderDto
import com.couchlist.app.core.data.remote.dto.MediaResultDto
import com.couchlist.app.core.data.remote.dto.MovieDetailDto
import com.couchlist.app.core.data.remote.dto.SeasonSummaryDto
import com.couchlist.app.core.data.remote.dto.TvDetailDto
import com.couchlist.app.core.data.remote.dto.TvSeasonDetailDto
import com.couchlist.app.core.domain.model.EpisodeMetadata
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaMetadata
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.ProviderCategory
import com.couchlist.app.core.domain.model.SeasonDetails
import com.couchlist.app.core.domain.model.SeasonMetadata
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

internal const val TMDB_SOURCE = "tmdb"

@Singleton
class TmdbMetadataProvider @Inject constructor(
    private val api: TmdbApi,
    private val settingsRepository: SettingsRepository,
) : MediaMetadataProvider, EpisodicMetadataProvider {
    override val source: String = TMDB_SOURCE
    override val supportedCategories: Set<MediaCategory> = setOf(
        MediaCategory.MOVIE,
        MediaCategory.TV,
    )

    override suspend fun search(query: String): List<MediaSearchResult> =
        api.searchMulti(query).results.mapNotNull { it.toDomain() }

    override suspend fun details(reference: MediaReference): MediaDetail {
        require(reference.source == source)
        val id = reference.externalId.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid TMDB ID: ${reference.externalId}")
        val providers = api.watchProviders(id, reference.category)
        return when (reference.category) {
            MediaCategory.MOVIE -> api.movie(id).toDomain(reference, providers)
            MediaCategory.TV -> api.tv(id).toDomain(reference, providers)
            else -> throw IllegalArgumentException("TMDB does not support ${reference.category}")
        }
    }

    override suspend fun seasonDetails(
        reference: MediaReference,
        seasonNumber: Int,
    ): SeasonDetails {
        require(reference.source == source && reference.category == MediaCategory.TV)
        val id = reference.externalId.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid TMDB ID: ${reference.externalId}")
        return api.tvSeason(id, seasonNumber).toDomain()
    }

    private suspend fun TmdbApi.watchProviders(
        id: Long,
        category: MediaCategory,
    ): List<WatchProvider> {
        val dto = when (category) {
            MediaCategory.MOVIE -> movieWatchProviders(id)
            MediaCategory.TV -> tvWatchProviders(id)
            else -> return emptyList()
        }
        val bundle = dto.results[settingsRepository.settings.first().providerRegion]
            ?: dto.results["US"]
            ?: return emptyList()
        return bundle.toDomain()
    }

    private fun MediaResultDto.toDomain(): MediaSearchResult? {
        val category = when (mediaType) {
            "movie" -> MediaCategory.MOVIE
            "tv" -> MediaCategory.TV
            else -> return null
        }
        return MediaSearchResult(
            reference = MediaReference(source, category, id.toString()),
            title = title ?: name.orEmpty(),
            artworkUri = TmdbImageUris.poster(posterPath),
            releaseYear = (releaseDate ?: firstAirDate).toYear(),
            description = overview,
            voteAverage = voteAverage,
        )
    }

    private fun MovieDetailDto.toDomain(
        reference: MediaReference,
        providers: List<WatchProvider>,
    ) = MediaDetail(
        reference = reference,
        title = title,
        originalTitle = originalTitle,
        description = overview,
        releaseDate = releaseDate,
        originalLanguage = originalLanguage,
        artworkUri = TmdbImageUris.poster(posterPath),
        backdropUri = TmdbImageUris.backdrop(backdropPath),
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = genres.map { it.name },
        providers = providers,
        metadata = MediaMetadata.Video(runtimeMinutes = runtime),
    )

    private fun TvSeasonDetailDto.toDomain() = SeasonDetails(
        season = SeasonMetadata(
            seasonNumber = seasonNumber,
            name = name,
            overview = overview,
            artworkUri = TmdbImageUris.poster(posterPath),
            airDate = airDate,
            episodeCount = episodes.size,
        ),
        episodes = episodes.map { episode ->
            EpisodeMetadata(
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                title = episode.name,
                overview = episode.overview,
                artworkUri = TmdbImageUris.still(episode.stillPath),
                airDate = episode.airDate,
                runtimeMinutes = episode.runtime,
            )
        },
    )

    private fun SeasonSummaryDto.toDomain() = SeasonMetadata(
        seasonNumber = seasonNumber,
        name = name,
        overview = overview,
        artworkUri = TmdbImageUris.poster(posterPath),
        airDate = airDate,
        episodeCount = episodeCount,
    )

    private fun TvDetailDto.toDomain(
        reference: MediaReference,
        providers: List<WatchProvider>,
    ) = MediaDetail(
        reference = reference,
        title = name,
        originalTitle = originalName,
        description = overview,
        releaseDate = firstAirDate,
        originalLanguage = originalLanguage,
        artworkUri = TmdbImageUris.poster(posterPath),
        backdropUri = TmdbImageUris.backdrop(backdropPath),
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = genres.map { it.name },
        providers = providers,
        metadata = MediaMetadata.Video(
            runtimeMinutes = episodeRunTime.firstOrNull(),
            seasons = seasons.map { it.toDomain() },
        ),
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
        logoUri = TmdbImageUris.logo(logoPath),
        category = category,
    )
}
