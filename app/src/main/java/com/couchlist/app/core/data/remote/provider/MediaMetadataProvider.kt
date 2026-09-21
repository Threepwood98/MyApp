package com.couchlist.app.core.data.remote.provider

import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.SeasonDetails

interface MediaMetadataProvider {
    val source: String
    val supportedCategories: Set<MediaCategory>

    suspend fun search(query: String): List<MediaSearchResult>

    suspend fun details(reference: MediaReference): MediaDetail
}

interface EpisodicMetadataProvider {
    val source: String

    suspend fun seasonDetails(reference: MediaReference, seasonNumber: Int): SeasonDetails
}
