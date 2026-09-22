package com.couchlist.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.repository.ExportImportRepositoryImpl
import com.couchlist.app.core.data.repository.LibraryRepositoryImpl
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaListType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportImportRepositoryIntegrationTest {

    private lateinit var database: CouchlistDatabase
    private lateinit var libraryRepository: LibraryRepositoryImpl
    private lateinit var exportImportRepository: ExportImportRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CouchlistDatabase::class.java,
        ).build()
        libraryRepository = LibraryRepositoryImpl(
            database = database,
            libraryItemDao = database.libraryItemDao(),
            mediaListDao = database.mediaListDao(),
        )
        exportImportRepository = ExportImportRepositoryImpl(
            database = database,
            mediaItemDao = database.mediaItemDao(),
            libraryItemDao = database.libraryItemDao(),
            mediaListDao = database.mediaListDao(),
            logEntryDao = database.logEntryDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun versionThreeRoundTripPreservesGroupsListsMembershipsAndPile() = runBlocking {
        val mediaId = insertMedia("42")
        val groupId = libraryRepository.createGroup("Movies")
        val listId = libraryRepository.createList(
            name = "Weekend",
            description = "Soon",
            type = MediaListType.TODO,
            groupId = groupId,
        )
        libraryRepository.addToList(listId, mediaId)
        libraryRepository.addToPile(mediaId)

        val exported = exportImportRepository.exportToJson()
        val nonCanonicalPileExport = exported.replace(
            oldValue = "\"name\": \"The Pile\"",
            newValue = "\"name\": \"Inbox\"",
        )
        assertTrue(nonCanonicalPileExport != exported)
        exportImportRepository.importFromJson(nonCanonicalPileExport)

        val groups = libraryRepository.observeGroups().first()
        val lists = libraryRepository.observeLists().first()
        val importedList = lists.single { it.list.name == "Weekend" }
        val pile = lists.single { it.list.type == MediaListType.PILE }

        assertEquals(1, groups.size)
        assertEquals(groups.single().id, importedList.list.groupId)
        assertEquals(1, importedList.itemCount)
        assertEquals("The Pile", pile.list.name)
        assertTrue(pile.list.isPinned)
        assertEquals(1, pile.itemCount)
        assertEquals(1, libraryRepository.observeListItems(importedList.list.id).first().size)
    }

    @Test
    fun versionOneImportResolvesLegacyReferences() = runBlocking {
        val legacyExport =
            """
            {
              "version": 1,
              "mediaItems": [
                {
                  "mediaType": "MOVIE",
                  "tmdbId": 550,
                  "title": "Fight Club"
                }
              ],
              "libraryItems": [
                {
                  "mediaType": "MOVIE",
                  "tmdbId": 550,
                  "status": "BACKLOG"
                }
              ],
              "lists": [
                {
                  "name": "Legacy list",
                  "type": "TODO"
                }
              ],
              "listMemberships": [
                {
                  "mediaType": "MOVIE",
                  "tmdbId": 550,
                  "listName": "Legacy list",
                  "listType": "TODO"
                }
              ],
              "logEntries": []
            }
            """.trimIndent()

        exportImportRepository.importFromJson(legacyExport)

        val lists = libraryRepository.observeLists().first()
        val legacyList = lists.single { it.list.name == "Legacy list" }
        assertEquals(1, legacyList.itemCount)
        assertEquals(1, libraryRepository.observeListItems(legacyList.list.id).first().size)
        assertEquals(1, lists.count { it.list.type == MediaListType.PILE })
    }

    @Test
    fun invalidReferencesAreRejectedBeforeExistingDataIsReplaced() = runBlocking {
        val mediaId = insertMedia("kept")
        libraryRepository.addToPile(mediaId)
        val invalidExport =
            """
            {
              "version": 2,
              "mediaItems": [],
              "libraryItems": [
                {
                  "source": "tmdb",
                  "category": "MOVIE",
                  "externalId": "missing",
                  "status": "BACKLOG"
                }
              ],
              "lists": [],
              "listMemberships": [],
              "logEntries": []
            }
            """.trimIndent()

        try {
            exportImportRepository.importFromJson(invalidExport)
            fail("Expected invalid import to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: validation happens before the replacement transaction.
        }

        assertNotNull(database.mediaItemDao().getById(mediaId))
        val pile = libraryRepository.observeLists().first().single { it.list.type == MediaListType.PILE }
        assertEquals(1, pile.itemCount)
    }

    private suspend fun insertMedia(externalId: String): Long =
        database.mediaItemDao().insertIgnore(
            MediaItemEntity(
                source = "tmdb",
                category = MediaCategory.MOVIE,
                externalId = externalId,
                title = "Title $externalId",
                originalTitle = null,
                description = null,
                artworkUri = null,
                backdropUri = null,
                releaseDate = null,
                originalLanguage = null,
                genres = null,
                lastRefreshedAt = null,
            ),
        )
}
