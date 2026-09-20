package com.couchlist.app.core.data.repository

import com.couchlist.app.core.data.remote.TmdbApi
import com.couchlist.app.core.data.remote.dto.CountryProvidersDto
import com.couchlist.app.core.data.remote.dto.MediaProviderDto
import com.couchlist.app.core.data.remote.dto.MediaResultDto
import com.couchlist.app.core.data.remote.dto.MovieDetailDto
import com.couchlist.app.core.data.remote.dto.TvDetailDto
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.ProviderCategory
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.MediaRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbMediaRepository @Inject constructor(
    private val api: TmdbApi,
) : MediaRepository {

    override suspend fun searchMulti(query: String): List<MediaSearchResult> =
        api.searchMulti(query).results.mapNotNull { it.toDomain() }

    override suspend fun details(id: Long, mediaType: MediaType): MediaDetail {
        val providers = api.watchProviders(id, mediaType)
        return when (mediaType) {
            MediaType.MOVIE -> api.movie(id).toDomain(mediaType, providers)
            MediaType.TV -> api.tv(id).toDomain(mediaType, providers)
        }
    }

    private suspend fun TmdbApi.watchProviders(id: Long, mediaType: MediaType): List<WatchProvider> {
        val dto = when (mediaType) {
            MediaType.MOVIE -> movieWatchProviders(id)
            MediaType.TV -> tvWatchProviders(id)
        }
        val bundle = dto.results[deviceCountry()]
            ?: dto.results["US"]
            ?: return emptyList()
        return bundle.toDomain()
    }

    private fun deviceCountry(): String =
        Locale.getDefault().country.ifBlank { "US" }

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
        )
    }

    private fun MovieDetailDto.toDomain(
        mediaType: MediaType,
        providers: List<WatchProvider>,
    ) = MediaDetail(
        id = id,
        mediaType = mediaType,
        title = title,
        overview = overview,
        releaseYear = releaseDate.toYear(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        providers = providers,
    )

    private fun TvDetailDto.toDomain(
        mediaType: MediaType,
        providers: List<WatchProvider>,
    ) = MediaDetail(
        id = id,
        mediaType = mediaType,
        title = name,
        overview = overview,
        releaseYear = firstAirDate.toYear(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        providers = providers,
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