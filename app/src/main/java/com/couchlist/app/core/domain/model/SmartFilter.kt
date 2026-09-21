package com.couchlist.app.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Dynamic filter criteria for smart lists. Items matching all non-null criteria
 * are included. Stored as JSON in the `smart_filter_json` column.
 */
@Serializable
data class SmartFilter(
    val statuses: Set<MediaStatus>? = null,
    val minRating: Int? = null,
    val maxRating: Int? = null,
    val favoriteOnly: Boolean? = null,
    val mediaTypes: List<MediaType>? = null,
    val genres: List<String>? = null,
) {
    fun matches(item: LibraryMedia): Boolean {
        statuses?.let { statuses ->
            if (item.library.status !in statuses) return false
        }
        minRating?.let { min ->
            val rating = item.library.personalRating ?: return false
            if (rating < min) return false
        }
        maxRating?.let { max ->
            val rating = item.library.personalRating ?: return false
            if (rating > max) return false
        }
        favoriteOnly?.let { fav ->
            if (fav && !item.library.favorite) return false
        }
        mediaTypes?.let { types ->
            if (item.media.mediaType !in types) return false
        }
        genres?.let { genres ->
            if (genres.none { it in item.media.genres }) return false
        }
        return true
    }

    fun description(): String = buildList {
        statuses?.let { add(it.joinToString { s -> s.displayName }) }
        minRating?.let { add("Rating $it+") }
        maxRating?.let { add("Rating ≤$it") }
        favoriteOnly?.let { if (it) add("Favorites only") }
        mediaTypes?.let { add(it.joinToString { t -> t.name }) }
        genres?.let { add(it.joinToString()) }
    }.joinToString(" · ").ifBlank { "All titles" }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        fun fromJson(jsonString: String?): SmartFilter? =
            jsonString?.takeIf { it.isNotBlank() }?.let {
                json.decodeFromString<SmartFilter>(it)
            }

        fun toJson(filter: SmartFilter): String = json.encodeToString(filter)
    }
}
