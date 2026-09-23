package com.couchlist.app.core.ui.sample

import com.couchlist.app.R
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaMetadata
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.domain.model.TrackingSummary

internal object SampleData {
    const val simulatedError = "The library could not be loaded."

    val movie = libraryMedia(
        id = 1,
        category = MediaCategory.MOVIE,
        title = "The Last Lantern",
        subtitle = "2024",
        artwork = R.drawable.sample_poster_movie,
        backdrop = R.drawable.sample_backdrop,
        description = "A quiet projectionist follows a trail of forgotten films across a sleeping coastal town.",
    )

    val tv = libraryMedia(
        id = 2,
        category = MediaCategory.TV,
        title = "Moss & Moonlight",
        subtitle = "2023",
        artwork = R.drawable.sample_poster_tv,
        progress = 0.42,
        trackingMode = TrackingMode.CHECKLIST,
    )

    val anime = libraryMedia(
        id = 3,
        category = MediaCategory.ANIME,
        title = "Cloud Station 7",
        subtitle = "2025",
        artwork = R.drawable.sample_poster_anime,
        progress = 0.76,
        trackingMode = TrackingMode.CHECKLIST,
    )

    val book = libraryMedia(
        id = 4,
        category = MediaCategory.BOOK,
        title = "A Field Guide to Small Wonders",
        artwork = R.drawable.sample_poster_book,
        progress = 0.31,
        trackingMode = TrackingMode.SIMPLE_COUNTER,
    )

    val manga = libraryMedia(
        id = 5,
        category = MediaCategory.MANGA,
        title = "Midnight Courier",
        artwork = R.drawable.sample_poster_manga,
        progress = null,
        trackingMode = TrackingMode.SIMPLE_COUNTER,
    )

    val comic = libraryMedia(
        id = 6,
        category = MediaCategory.COMIC,
        title = "Signal City",
        artwork = R.drawable.sample_poster_comic,
        progress = 1.0,
        trackingMode = TrackingMode.CHECKLIST,
        trackingState = TrackingState.COMPLETED,
    )

    val game = libraryMedia(
        id = 7,
        category = MediaCategory.VIDEO_GAME,
        title = "Afterlight Valley",
        artwork = R.drawable.sample_poster_game,
        trackingMode = TrackingMode.JOURNAL,
    )

    val missingArtwork = libraryMedia(
        id = 8,
        category = MediaCategory.CUSTOM,
        title = "An Item Without Artwork",
    )

    val longTitle = libraryMedia(
        id = 9,
        category = MediaCategory.BOOK,
        title = "The Extremely Long and Remarkably Specific Chronicle of a Very Small Library",
        artwork = R.drawable.sample_poster_book,
        description = "A deliberately long description used to verify wrapping, truncation, and large font behavior without relying on remote content.",
    )

    val allMedia = listOf(movie, tv, anime, book, manga, comic, game)
    val enjoying = listOf(tv, anime, book, manga, game)
    val pileItems = listOf(movie, anime, book, game)
    val largeLibrary = List(28) { index ->
        val source = allMedia[index % allMedia.size]
        val id = 100L + index
        source.copy(
            media = source.media.copy(
                id = id,
                reference = source.media.reference.copy(externalId = id.toString()),
            ),
            library = source.library.copy(id = id, mediaId = id),
            tracking = source.tracking?.copy(sessionId = id),
        )
    }

    val pile = listSummary(
        id = 1,
        name = "The Pile",
        type = MediaListType.PILE,
        itemCount = pileItems.size,
        coverArtwork = movie.media.artworkUri,
        isPinned = true,
    )

    val favoriteLists = listOf(
        listSummary(2, "Rainy day favorites", MediaListType.COLLECTION, 12, tv.media.artworkUri, true),
        listSummary(3, "Read next", MediaListType.TODO, 8, book.media.artworkUri, true),
        listSummary(4, "Co-op nights", MediaListType.COLLECTION, 5, game.media.artworkUri),
    )

    private fun libraryMedia(
        id: Long,
        category: MediaCategory,
        title: String,
        subtitle: String? = null,
        artwork: Int? = null,
        backdrop: Int? = null,
        description: String? = null,
        progress: Double? = null,
        trackingMode: TrackingMode? = null,
        trackingState: TrackingState = TrackingState.ACTIVE,
    ): LibraryMedia {
        val timestamp = 1_725_000_000_000L + id
        return LibraryMedia(
            media = MediaItem(
                id = id,
                reference = MediaReference("sample", category, id.toString()),
                title = title,
                originalTitle = null,
                description = description,
                artworkUri = artwork?.resourceUri,
                backdropUri = backdrop?.resourceUri,
                releaseDate = subtitle,
                originalLanguage = null,
                externalRating = 0.0,
                externalVoteCount = 0,
                genres = emptyList(),
                metadata = when (category) {
                    MediaCategory.MOVIE, MediaCategory.TV, MediaCategory.ANIME ->
                        MediaMetadata.Video(runtimeMinutes = null)
                    else -> MediaMetadata.None
                },
                lastRefreshedAt = null,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
            library = LibraryItem(
                id = id,
                mediaId = id,
                personalRating = null,
                favorite = false,
                notes = null,
                addedAt = timestamp,
                updatedAt = timestamp,
            ),
            tracking = trackingMode?.let {
                TrackingSummary(
                    sessionId = id,
                    mode = it,
                    state = trackingState,
                    progress = progress,
                    startedAt = timestamp,
                    endedAt = timestamp.takeIf { trackingState == TrackingState.COMPLETED },
                    updatedAt = timestamp,
                )
            },
        )
    }

    private fun listSummary(
        id: Long,
        name: String,
        type: MediaListType,
        itemCount: Int,
        coverArtwork: String?,
        isPinned: Boolean = false,
    ) = MediaListSummary(
        list = MediaList(
            id = id,
            name = name,
            description = null,
            type = type,
            groupId = null,
            coverMediaId = null,
            isPinned = isPinned,
            sortOrder = id.toInt(),
            smartFilterJson = null,
            createdAt = 1_725_000_000_000L + id,
            updatedAt = 1_725_000_000_000L + id,
        ),
        itemCount = itemCount,
        coverArtworkUri = coverArtwork,
    )

    private val Int.resourceUri: String
        get() = "android.resource://com.couchlist.app/$this"
}
