package com.couchlist.app.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchProvidersDto(
    val id: Long = 0,
    val results: Map<String, CountryProvidersDto> = emptyMap(),
)

@Serializable
data class CountryProvidersDto(
    val link: String? = null,
    val flatrate: List<MediaProviderDto>? = null,
    val rent: List<MediaProviderDto>? = null,
    val buy: List<MediaProviderDto>? = null,
)

@Serializable
data class MediaProviderDto(
    @SerialName("display_priority") val displayPriority: Int = 0,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("provider_id") val providerId: Long = 0,
    @SerialName("provider_name") val providerName: String = "",
)