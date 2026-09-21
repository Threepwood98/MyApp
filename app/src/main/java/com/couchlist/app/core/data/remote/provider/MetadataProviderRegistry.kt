package com.couchlist.app.core.data.remote.provider

import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.SeasonDetails
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataProviderRegistry @Inject constructor(
    providers: Set<@JvmSuppressWildcards MediaMetadataProvider>,
    episodicProviders: Set<@JvmSuppressWildcards EpisodicMetadataProvider>,
) {
    private val providersBySource = providers.associateByUniqueSource { it.source }
    private val episodicProvidersBySource = episodicProviders.associateByUniqueSource { it.source }

    val supportedCategories: Set<MediaCategory> = providers
        .flatMapTo(linkedSetOf()) { it.supportedCategories }

    suspend fun search(query: String): List<MediaSearchResult> = providersBySource
        .toSortedMap()
        .values
        .flatMap { it.search(query) }

    suspend fun details(reference: MediaReference): MediaDetail {
        val provider = providersBySource[reference.source]
            ?: throw IllegalArgumentException("Unknown metadata source: ${reference.source}")
        require(reference.category in provider.supportedCategories) {
            "${reference.category} is not supported by ${reference.source}"
        }
        return provider.details(reference)
    }

    suspend fun seasonDetails(
        reference: MediaReference,
        seasonNumber: Int,
    ): SeasonDetails {
        val provider = episodicProvidersBySource[reference.source]
            ?: throw IllegalArgumentException("Source ${reference.source} has no episodic metadata")
        return provider.seasonDetails(reference, seasonNumber)
    }
}

private inline fun <T> Set<T>.associateByUniqueSource(source: (T) -> String): Map<String, T> {
    val result = associateBy(source)
    require(result.size == size) { "Metadata provider source IDs must be unique" }
    return result
}
