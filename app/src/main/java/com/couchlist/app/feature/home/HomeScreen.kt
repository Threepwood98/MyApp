package com.couchlist.app.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaMetadata
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.ui.theme.CouchlistTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onItemClick: (LibraryMedia) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Couchlist") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        HomeContent(
            uiState = uiState,
            onSearchClick = onSearchClick,
            onItemClick = onItemClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onSearchClick: () -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isEmpty) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Your couch is ready", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Save a movie or show and Couchlist will keep your next watch close.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onSearchClick) { Text(text = "Find something") }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Settle in.",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Your next watch, recent saves, and finished favorites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                DashboardSection(
                    title = "Continue watching",
                    items = uiState.continueWatching,
                    onItemClick = onItemClick,
                )
            }
        }
        if (uiState.recentlyAdded.isNotEmpty()) {
            item {
                DashboardSection(
                    title = "Recently added",
                    items = uiState.recentlyAdded,
                    onItemClick = onItemClick,
                )
            }
        }
        if (uiState.recentlyCompleted.isNotEmpty()) {
            item {
                DashboardSection(
                    title = "Recently completed",
                    items = uiState.recentlyCompleted,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    items: List<LibraryMedia>,
    onItemClick: (LibraryMedia) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = items, key = { it.library.id }) { item ->
                DashboardCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun DashboardCard(
    item: LibraryMedia,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick),
    ) {
        if (item.media.artworkUri != null) {
            AsyncImage(
                model = item.media.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = item.media.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (item.library.status) {
                    MediaStatus.WATCHING -> "In progress"
                    MediaStatus.COMPLETED -> "Completed"
                    else -> DateUtils.getRelativeTimeSpanString(item.library.addedAt).toString()
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    CouchlistTheme {
        HomeContent(
            uiState = HomeUiState(
                continueWatching = listOf(sampleLibraryMedia),
                recentlyAdded = listOf(sampleLibraryMedia),
            ),
            onSearchClick = {},
            onItemClick = {},
        )
    }
}

private val sampleLibraryMedia = LibraryMedia(
    media = MediaItem(
        id = 1,
        reference = MediaReference("tmdb", MediaCategory.MOVIE, "550"),
        title = "Fight Club",
        originalTitle = "Fight Club",
        description = null,
        artworkUri = null,
        backdropUri = null,
        releaseDate = "1999-10-15",
        originalLanguage = "en",
        externalRating = 8.4,
        externalVoteCount = 0,
        genres = listOf("Drama"),
        metadata = MediaMetadata.Video(runtimeMinutes = 139),
        lastRefreshedAt = null,
        createdAt = 1,
        updatedAt = 1,
    ),
    library = LibraryItem(
        id = 1,
        mediaId = 1,
        status = MediaStatus.WATCHING,
        progress = null,
        personalRating = null,
        favorite = false,
        notes = null,
        addedAt = 1,
        startedAt = 1,
        completedAt = null,
        updatedAt = 1,
    ),
)
