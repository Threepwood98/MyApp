package com.couchlist.app.feature.lists

import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.ListGroup
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaMetadata
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ListsUiStateTest {

    @Test
    fun `pile pinned grouped and remaining lists form distinct sections`() {
        val group = ListGroup(7, "Genres", 0, 1, 1)
        val pile = summary(1, MediaListType.PILE, pinned = true)
        val pinned = summary(2, MediaListType.COLLECTION, pinned = true)
        val grouped = summary(3, MediaListType.TODO, groupId = group.id)
        val remaining = summary(4, MediaListType.COLLECTION)

        val state = buildListsUiState(
            summaries = listOf(remaining, grouped, pinned, pile),
            groups = listOf(group),
        )

        assertEquals(1L, state.pile?.list?.id)
        assertEquals(listOf(2L), state.pinnedLists.map { it.list.id })
        assertEquals(listOf(3L), state.groups.single().lists.map { it.list.id })
        assertEquals(listOf(4L), state.ungroupedLists.map { it.list.id })
    }

    @Test
    fun `todo list hides completed items until requested`() {
        val active = libraryMedia(1, MediaStatus.BACKLOG)
        val completed = libraryMedia(2, MediaStatus.COMPLETED)
        val state = ListDetailUiState(
            list = summary(5, MediaListType.TODO).list,
            items = listOf(active, completed),
        )

        assertEquals(listOf(1L), state.visibleItems.map { it.library.id })
        assertEquals(2, state.copy(showCompleted = true).visibleItems.size)
    }

    private fun summary(
        id: Long,
        type: MediaListType,
        pinned: Boolean = false,
        groupId: Long? = null,
    ) = MediaListSummary(
        list = MediaList(
            id = id,
            name = "List $id",
            description = null,
            type = type,
            groupId = groupId,
            coverMediaId = null,
            isPinned = pinned,
            sortOrder = id.toInt(),
            smartFilterJson = null,
            createdAt = 1,
            updatedAt = 1,
        ),
        itemCount = 0,
        coverArtworkUri = null,
    )

    private fun libraryMedia(id: Long, status: MediaStatus) = LibraryMedia(
        media = MediaItem(
            id = id,
            reference = MediaReference("tmdb", MediaCategory.MOVIE, id.toString()),
            title = "Title $id",
            originalTitle = null,
            description = null,
            artworkUri = null,
            backdropUri = null,
            releaseDate = null,
            originalLanguage = null,
            externalRating = 0.0,
            externalVoteCount = 0,
            genres = emptyList(),
            metadata = MediaMetadata.None,
            lastRefreshedAt = null,
            createdAt = 1,
            updatedAt = 1,
        ),
        library = LibraryItem(
            id = id,
            mediaId = id,
            status = status,
            progress = null,
            personalRating = null,
            favorite = false,
            notes = null,
            addedAt = 1,
            startedAt = null,
            completedAt = null,
            updatedAt = 1,
        ),
    )
}
