package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.MediaType

interface MediaRepository {
    suspend fun searchMulti(query: String): List<MediaSearchResult>

    suspend fun details(id: Long, mediaType: MediaType): MediaDetail
}