package com.couchlist.app.core.ui.components

object TmdbImages {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    fun posterUrl(path: String?, size: String = "w500"): String? =
        path?.let { BASE_URL + size + it }

    fun backdropUrl(path: String?, size: String = "w780"): String? =
        path?.let { BASE_URL + size + it }

    fun stillUrl(path: String?, size: String = "w300"): String? =
        path?.let { BASE_URL + size + it }

    fun logoUrl(path: String?, size: String = "w92"): String? =
        path?.let { BASE_URL + size + it }
}
