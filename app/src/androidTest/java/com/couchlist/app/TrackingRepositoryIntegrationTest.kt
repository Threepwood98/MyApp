package com.couchlist.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.repository.TrackingRepositoryImpl
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.TrackingDetails
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingRepositoryIntegrationTest {
    private lateinit var database: CouchlistDatabase
    private lateinit var repository: TrackingRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CouchlistDatabase::class.java,
        ).build()
        repository = TrackingRepositoryImpl(
            database = database,
            libraryItemDao = database.libraryItemDao(),
            trackingDao = database.trackingDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun counterTransitionsDriveEnjoyingAndKeepUnclampedProgress() = runBlocking {
        val mediaId = insertMedia(MediaCategory.BOOK, "book")
        val libraryItemId = repository.start(
            mediaId = mediaId,
            mode = TrackingMode.SIMPLE_COUNTER,
            counterTotal = 100.0,
            counterUnit = "pages",
        )

        repository.updateCounter(libraryItemId, current = 125.0, total = 100.0, unit = "pages")

        val active = repository.observeCurrent(libraryItemId).first()
        assertEquals(TrackingState.ACTIVE, active?.state)
        assertEquals(1.25, active?.progress ?: 0.0, 0.0)
        assertEquals(1.25, repository.observeEnjoying().first().single().progress ?: 0.0, 0.0)

        repository.pause(libraryItemId)
        assertTrue(repository.observeEnjoying().first().isEmpty())
        repository.resume(libraryItemId)
        repository.complete(libraryItemId)
        val completed = repository.observeCurrent(libraryItemId).first()
        assertEquals(TrackingState.COMPLETED, completed?.state)

        repository.start(mediaId, TrackingMode.SIMPLE_COUNTER, counterTotal = 100.0, counterUnit = "pages")
        val restarted = repository.observeCurrent(libraryItemId).first()
        assertEquals(TrackingState.ACTIVE, restarted?.state)
        assertNotEquals(completed?.id, restarted?.id)
        val sessions = database.trackingDao().getAllSessions()
        assertEquals(2, sessions.size)
        assertEquals(1, sessions.count { it.currentSlot == 1 })
    }

    @Test
    fun checklistQuickLogAndJournalPersistModeSpecificDetails() = runBlocking {
        val checklistMediaId = insertMedia(MediaCategory.TV, "show")
        val checklistLibraryId = repository.start(checklistMediaId, TrackingMode.CHECKLIST)
        val groupId = repository.addCheckpoint(
            libraryItemId = checklistLibraryId,
            label = "Season 1",
            kind = com.couchlist.app.core.domain.model.TrackingCheckpointKind.GROUP,
            stableKey = "season:1",
        )
        val episodeId = repository.addCheckpoint(
            libraryItemId = checklistLibraryId,
            label = "Episode 1",
            parentId = groupId,
            stableKey = "episode:s1:e1",
        )
        repository.setCheckpointCompleted(episodeId, true)
        val checklist = repository.observeCurrent(checklistLibraryId).first()
        assertEquals(1.0, checklist?.progress ?: 0.0, 0.0)

        val quickMediaId = insertMedia(MediaCategory.MOVIE, "quick")
        val quickLibraryId = repository.start(quickMediaId, TrackingMode.QUICK_LOG)
        repository.addQuickLog(quickLibraryId, "Second viewing")
        val quickDetails = repository.observeCurrent(quickLibraryId).first()?.details as TrackingDetails.QuickLog
        assertEquals("Second viewing", quickDetails.entries.single().note)

        val journalMediaId = insertMedia(MediaCategory.VIDEO_GAME, "game")
        val journalLibraryId = repository.start(journalMediaId, TrackingMode.JOURNAL)
        repository.addJournalEntry(journalLibraryId, "Reached Act II", "After the bridge")
        val journalDetails = repository.observeCurrent(journalLibraryId).first()?.details as TrackingDetails.Journal
        assertEquals("Reached Act II", journalDetails.entries.single().title)
        assertNull(journalDetails.entries.single().imageUri)
    }

    @Test
    fun completedChecklistCannotBeEdited() = runBlocking {
        val mediaId = insertMedia(MediaCategory.TV, "completed-show")
        val libraryItemId = repository.start(mediaId, TrackingMode.CHECKLIST)
        val checkpointId = repository.addCheckpoint(libraryItemId, "Episode 1")
        repository.complete(libraryItemId)

        val result = runCatching {
            repository.setCheckpointCompleted(checkpointId, true)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        val details = repository.observeCurrent(libraryItemId).first()?.details as TrackingDetails.Checklist
        assertNull(details.checkpoints.single().completedAt)
    }

    private suspend fun insertMedia(category: MediaCategory, externalId: String): Long =
        database.mediaItemDao().insertIgnore(
            MediaItemEntity(
                source = "test",
                category = category,
                externalId = externalId,
                title = externalId,
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
