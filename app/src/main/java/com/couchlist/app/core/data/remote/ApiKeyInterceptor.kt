package com.couchlist.app.core.data.remote

import com.couchlist.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = BuildConfig.TMDB_API_KEY
        val request = if (apiKey.isBlank()) {
            chain.request()
        } else {
            val url = chain.request().url.newBuilder()
                .addQueryParameter("api_key", apiKey)
                .build()
            chain.request().newBuilder().url(url).build()
        }
        return chain.proceed(request)
    }
}