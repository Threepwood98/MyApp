package com.couchlist.app.core.domain.model

data class WatchProvider(
    val providerId: Long,
    val name: String,
    val logoUri: String?,
    val category: ProviderCategory,
)

enum class ProviderCategory {
    FLATRATE,
    RENT,
    BUY,
}
