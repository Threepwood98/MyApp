package com.couchlist.app.core.data.remote

import com.couchlist.app.core.data.remote.dto.MovieDetailDto
import com.couchlist.app.core.data.remote.dto.MultiSearchDto
import com.couchlist.app.core.data.remote.dto.TvDetailDto
import com.couchlist.app.core.data.remote.dto.WatchProvidersDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): MultiSearchDto

    @GET("movie/{id}")
    suspend fun movie(
        @Path("id") id: Long,
    ): MovieDetailDto

    @GET("tv/{id}")
    suspend fun tv(
        @Path("id") id: Long,
    ): TvDetailDto

    @GET("movie/{id}/watch/providers")
    suspend fun movieWatchProviders(
        @Path("id") id: Long,
    ): WatchProvidersDto

    @GET("tv/{id}/watch/providers")
    suspend fun tvWatchProviders(
        @Path("id") id: Long,
    ): WatchProvidersDto
}