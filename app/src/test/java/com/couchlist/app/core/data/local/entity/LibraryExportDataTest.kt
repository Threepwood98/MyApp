package com.couchlist.app.core.data.local.entity

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LibraryExportDataTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun `v4 export includes source category and externalId`() {
        val export = LibraryExportData(
            mediaItems = listOf(
                ExportMediaItem(
                    source = "tmdb",
                    category = "MOVIE",
                    externalId = "12345",
                    title = "Test Movie",
                ),
            ),
            libraryItems = emptyList(),
            lists = emptyList(),
            listMemberships = emptyList(),
            logEntries = emptyList(),
        )
        val encoded = json.encodeToString(LibraryExportData.serializer(), export)
        assertTrue(encoded.contains("\"source\""))
        assertTrue(encoded.contains("\"category\""))
        assertTrue(encoded.contains("\"externalId\""))
        assertEquals(4, export.version)
    }

    @Test
    fun `v1 export without source fields still parses`() {
        val v1Json = """
            {
                "version": 1,
                "mediaItems": [
                    {
                        "mediaType": "MOVIE",
                        "tmdbId": 12345,
                        "title": "Old Movie"
                    }
                ],
                "libraryItems": [],
                "lists": [],
                "listMemberships": [],
                "logEntries": []
            }
        """.trimIndent()
        val data = json.decodeFromString(LibraryExportData.serializer(), v1Json)
        assertEquals(1, data.version)
        assertEquals(1, data.mediaItems.size)
        val item = data.mediaItems[0]
        assertEquals("tmdb", item.resolvedSource())
        assertEquals("MOVIE", item.resolvedCategory())
        assertEquals("12345", item.resolvedExternalId())
        assertNull(item.source)
        assertEquals(12345L, item.tmdbId)
    }

    @Test
    fun `v3 export round trip preserves all fields`() {
        val export = LibraryExportData(
            version = 3,
            mediaItems = listOf(
                ExportMediaItem(
                    source = "tmdb",
                    category = "TV",
                    externalId = "67890",
                    title = "Test Show",
                    runtimeMinutes = 45,
                    genres = "Drama, Sci-Fi",
                ),
            ),
            libraryItems = listOf(
                ExportLibraryItem(
                    source = "tmdb",
                    category = "TV",
                    externalId = "67890",
                    status = "WATCHING",
                    progress = 0.5,
                ),
            ),
            lists = emptyList(),
            listMemberships = emptyList(),
            logEntries = emptyList(),
        )
        val encoded = json.encodeToString(LibraryExportData.serializer(), export)
        val decoded = json.decodeFromString(LibraryExportData.serializer(), encoded)

        assertEquals(3, decoded.version)
        val item = decoded.mediaItems[0]
        assertEquals("tmdb", item.source)
        assertEquals("TV", item.category)
        assertEquals("67890", item.externalId)
        assertEquals(45, item.runtimeMinutes)
        assertEquals("Drama, Sci-Fi", item.genres)

        val libItem = decoded.libraryItems[0]
        assertEquals("tmdb", libItem.source)
        assertEquals("TV", libItem.category)
        assertEquals("67890", libItem.externalId)
        assertEquals("WATCHING", libItem.status)
    }

    @Test
    fun `ExportListMembership resolvedSource falls back to tmdb`() {
        val membership = ExportListMembership(
            listName = "Watchlist",
            listType = "TODO",
            mediaType = "MOVIE",
            tmdbId = 123L,
        )
        assertEquals("tmdb", membership.resolvedSource())
        assertEquals("MOVIE", membership.resolvedCategory())
        assertEquals("123", membership.resolvedExternalId())
    }

    @Test
    fun `ExportLogEntry resolvedSource prefers v2 fields`() {
        val entry = ExportLogEntry(
            source = "custom",
            category = "BOOK",
            externalId = "abc",
            action = "MOVIE_WATCHED",
            date = 1000L,
        )
        assertEquals("custom", entry.resolvedSource())
        assertEquals("BOOK", entry.resolvedCategory())
        assertEquals("abc", entry.resolvedExternalId())
    }

    @Test
    fun `v2 export defaults groups to empty`() {
        val encoded = """
            {
                "version": 2,
                "mediaItems": [],
                "libraryItems": [],
                "lists": [],
                "listMemberships": [],
                "logEntries": []
            }
        """.trimIndent()

        val decoded = json.decodeFromString(LibraryExportData.serializer(), encoded)

        assertEquals(2, decoded.version)
        assertEquals(emptyList<ExportListGroup>(), decoded.listGroups)
    }

    @Test
    fun `v3 export preserves group and stable list references`() {
        val export = LibraryExportData(
            version = 3,
            mediaItems = emptyList(),
            libraryItems = emptyList(),
            listGroups = listOf(ExportListGroup(id = 4, name = "Weekend", sortOrder = 1)),
            lists = listOf(
                ExportList(id = 8, name = "Movies", type = "TODO", groupId = 4),
            ),
            listMemberships = listOf(
                ExportListMembership(
                    listId = 8,
                    source = "tmdb",
                    category = "MOVIE",
                    externalId = "550",
                    addedAt = 42,
                ),
            ),
            logEntries = emptyList(),
        )

        val decoded = json.decodeFromString(
            LibraryExportData.serializer(),
            json.encodeToString(LibraryExportData.serializer(), export),
        )

        assertEquals(4L, decoded.listGroups.single().id)
        assertEquals(4L, decoded.lists.single().groupId)
        assertEquals(8L, decoded.listMemberships.single().listId)
        assertEquals(42L, decoded.listMemberships.single().addedAt)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
