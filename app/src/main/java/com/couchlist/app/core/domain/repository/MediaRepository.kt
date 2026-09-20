package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.SeasonDetails

interface MediaRepository {
    suspend fun searchMulti(query: String): Result<List<MediaSearchResult>>

    suspend fun details(id: Long, mediaType: MediaType): Result<MediaDetail>

    suspend fun seasonDetails(tvId: Long, seasonNumber: Int): Result<SeasonDetails>
}
