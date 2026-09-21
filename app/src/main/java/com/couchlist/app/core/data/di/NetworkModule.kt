package com.couchlist.app.core.data.di

import com.couchlist.app.BuildConfig
import com.couchlist.app.core.data.remote.ApiKeyInterceptor
import com.couchlist.app.core.data.remote.TmdbApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @TmdbNetwork
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    redactQueryParams("api_key")
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @TmdbNetwork
    fun provideRetrofit(@TmdbNetwork okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()

    @Provides
    @Singleton
    fun provideTmdbApi(@TmdbNetwork retrofit: Retrofit): TmdbApi =
        retrofit.create(TmdbApi::class.java)
}
