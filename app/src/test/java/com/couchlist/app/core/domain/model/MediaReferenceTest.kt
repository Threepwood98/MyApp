package com.couchlist.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaReferenceTest {

    @Test
    fun `stableKey uses source category and externalId`() {
        val ref = MediaReference("tmdb", MediaCategory.MOVIE, "12345")
        assertEquals("tmdb:MOVIE:12345", ref.stableKey)
    }

    @Test
    fun `references with same fields are equal`() {
        val a = MediaReference("tmdb", MediaCategory.TV, "67890")
        val b = MediaReference("tmdb", MediaCategory.TV, "67890")
        assertEquals(a, b)
        assertEquals(a.stableKey, b.stableKey)
    }

    @Test
    fun `references with different externalId are not equal`() {
        val a = MediaReference("tmdb", MediaCategory.MOVIE, "111")
        val b = MediaReference("tmdb", MediaCategory.MOVIE, "222")
        assertNotEquals(a, b)
    }

    @Test
    fun `references with different source are not equal`() {
        val a = MediaReference("tmdb", MediaCategory.MOVIE, "123")
        val b = MediaReference("custom", MediaCategory.MOVIE, "123")
        assertNotEquals(a, b)
    }
}
