package com.couchlist.app.core.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFilterTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `categories serialized as mediaTypes for JSON compat`() {
        val filter = SmartFilter(
            categories = listOf(MediaCategory.MOVIE, MediaCategory.TV),
        )
        val encoded = json.encodeToString(SmartFilter.serializer(), filter)
        assertTrue(encoded.contains("\"mediaTypes\""))
        assertTrue(encoded.contains("MOVIE"))
        assertTrue(encoded.contains("TV"))
    }

    @Test
    fun `old JSON with mediaTypes key decodes correctly`() {
        val oldJson = """{"mediaTypes":["MOVIE"],"genres":null}"""
        val filter = json.decodeFromString(SmartFilter.serializer(), oldJson)
        assertEquals(listOf(MediaCategory.MOVIE), filter.categories)
    }

    @Test
    fun `null categories produces null`() {
        val filter = SmartFilter()
        assertNull(filter.categories)
        val encoded = json.encodeToString(SmartFilter.serializer(), filter)
        val decoded = json.decodeFromString(SmartFilter.serializer(), encoded)
        assertNull(decoded.categories)
    }

    @Test
    fun `MediaCategory valueOf works for all values`() {
        MediaCategory.entries.forEach { category ->
            assertEquals(category, MediaCategory.valueOf(category.name))
        }
    }

    @Test
    fun `SmartFilter round trip preserves all fields`() {
        val filter = SmartFilter(
            statuses = setOf(MediaStatus.WATCHING, MediaStatus.COMPLETED),
            minRating = 7,
            maxRating = 10,
            favoriteOnly = true,
            categories = listOf(MediaCategory.MOVIE),
            genres = listOf("Drama", "Sci-Fi"),
        )
        val encoded = json.encodeToString(SmartFilter.serializer(), filter)
        val decoded = json.decodeFromString(SmartFilter.serializer(), encoded)
        assertEquals(filter.statuses, decoded.statuses)
        assertEquals(filter.minRating, decoded.minRating)
        assertEquals(filter.maxRating, decoded.maxRating)
        assertEquals(filter.favoriteOnly, decoded.favoriteOnly)
        assertEquals(filter.categories, decoded.categories)
        assertEquals(filter.genres, decoded.genres)
    }
}
