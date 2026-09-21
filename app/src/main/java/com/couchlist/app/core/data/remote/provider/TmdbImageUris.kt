package com.couchlist.app.core.data.remote.provider

internal object TmdbImageUris {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    fun poster(pathOrUri: String?): String? = resolve(pathOrUri, "w500")

    fun backdrop(pathOrUri: String?): String? = resolve(pathOrUri, "w780")

    fun still(pathOrUri: String?): String? = resolve(pathOrUri, "w300")

    fun logo(pathOrUri: String?): String? = resolve(pathOrUri, "w92")

    private fun resolve(pathOrUri: String?, size: String): String? = pathOrUri
        ?.takeIf { it.isNotBlank() }
        ?.let { value ->
            if ("://" in value) value else BASE_URL + size + value
        }
}
