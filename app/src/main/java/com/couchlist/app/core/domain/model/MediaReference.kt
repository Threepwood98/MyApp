package com.couchlist.app.core.domain.model

data class MediaReference(
    val source: String,
    val category: MediaCategory,
    val externalId: String,
) {
    init {
        require(source.isNotBlank())
        require(externalId.isNotBlank())
    }

    val stableKey: String
        get() = "$source:${category.name}:$externalId"
}
