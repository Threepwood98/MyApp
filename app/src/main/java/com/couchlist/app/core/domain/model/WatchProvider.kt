package com.couchlist.app.core.domain.model

data class WatchProvider(
    val providerId: Long,
    val name: String,
    val logoPath: String?,
    val category: ProviderCategory,
)

enum class ProviderCategory {
    FLATRATE,
    RENT,
    BUY,
}