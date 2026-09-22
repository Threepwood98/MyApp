package com.couchlist.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.repository.LibraryRepositoryImpl
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaListType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryRepositoryIntegrationTest {

    private lateinit var database: CouchlistDatabase
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CouchlistDatabase::class.java,
        ).build()
        repository = LibraryRepositoryImpl(
            database = database,
            libraryItemDao = database.libraryItemDao(),
            mediaListDao = database.mediaListDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pileIsCreatedOnceAndProtected() = runBlocking {
        val firstMediaId = insertMedia("1")
        val secondMediaId = insertMedia("2")

        repository.addToPile(firstMediaId)
        repository.addToPile(secondMediaId)

        val pile = repository.observeLists().first().single { it.list.type == MediaListType.PILE }
        assertEquals("The Pile", pile.list.name)
        assertTrue(pile.list.isPinned)
        assertEquals(2, pile.itemCount)

        val groupId = repository.createGroup("Group")
        repository.setListPinned(pile.list.id, false)
        repository.setListGroup(pile.list.id, groupId)
        repository.updateList(
            listId = pile.list.id,
            name = "Renamed",
            description = "Changed",
            type = MediaListType.COLLECTION,
            groupId = groupId,
        )
        repository.deleteList(pile.list.id)

        val protectedPile = repository.observeList(pile.list.id).first()
        assertNotNull(protectedPile)
        assertEquals("The Pile", protectedPile?.name)
        assertEquals(MediaListType.PILE, protectedPile?.type)
        assertTrue(protectedPile?.isPinned == true)
        assertNull(protectedPile?.groupId)
    }

    @Test
    fun listMembershipsCanBeCopiedMovedAndRemovedWithoutDeletingLibraryItem() = runBlocking {
        val mediaId = insertMedia("3")
        val groupId = repository.createGroup("Queue")
        val sourceId = repository.createList(
            name = " To watch ",
            description = " Later ",
            type = MediaListType.TODO,
            groupId = groupId,
        )
        val targetId = repository.createList(
            name = "Favorites",
            description = null,
            type = MediaListType.COLLECTION,
        )
        val libraryItemId = repository.addToList(sourceId, mediaId)

        repository.copyToList(targetId, libraryItemId)
        assertEquals(1, repository.observeListItems(sourceId).first().size)
        assertEquals(1, repository.observeListItems(targetId).first().size)

        repository.moveToList(sourceId, targetId, libraryItemId)
        assertTrue(repository.observeListItems(sourceId).first().isEmpty())
        assertEquals(1, repository.observeListItems(targetId).first().size)
        assertNotNull(repository.observeEntry(mediaId).first())

        val duplicateId = repository.duplicateList(targetId)
        assertNotNull(duplicateId)
        assertEquals(1, repository.observeListItems(duplicateId!!).first().size)

        repository.deleteGroup(groupId)
        val source = repository.observeList(sourceId).first()
        assertEquals("To watch", source?.name)
        assertEquals("Later", source?.description)
        assertNull(source?.groupId)

        repository.removeFromList(targetId, libraryItemId)
        assertTrue(repository.observeListItems(targetId).first().isEmpty())
        assertNotNull(repository.observeEntry(mediaId).first())
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
