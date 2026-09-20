package com.couchlist.app.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.couchlist.app.core.domain.model.LibraryItem
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.nextStatus
import com.couchlist.app.core.ui.components.TmdbImages
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = MediaStatus.entries
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is HomeEvent.ShowMessage ->
                        snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Couchlist") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, status ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = status.displayName) },
                    )
                }
            }
            val items = when (tabs[selectedTab]) {
                MediaStatus.BACKLOG -> uiState.backlog
                MediaStatus.WATCHING -> uiState.watching
                MediaStatus.COMPLETED -> uiState.completed
                MediaStatus.ABANDONED -> uiState.abandoned
            }
            StatusList(
                status = tabs[selectedTab],
                items = items,
                onAdvance = viewModel::onAdvanceStatus,
                onRemove = viewModel::onRemove,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun StatusList(
    status: MediaStatus,
    items: List<LibraryMedia>,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissedItems = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(items) {
        dismissedItems.keys.removeAll { key -> items.none { it.library.id == key } }
    }
    val visibleItems = items.filterNot { dismissedItems.containsKey(it.library.id) }

    if (visibleItems.isEmpty() && items.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${status.displayName} is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (status) {
                    MediaStatus.BACKLOG -> "Search for something to add."
                    MediaStatus.WATCHING -> "Swipe right on a title to start watching it."
                    MediaStatus.COMPLETED -> "Advance a title to mark it completed."
                    MediaStatus.ABANDONED -> "Titles you stop watching appear here."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items = visibleItems, key = { it.library.id }) { item ->
                SwipeableMediaItem(
                    item = item,
                    onAdvance = onAdvance,
                    onRemove = onRemove,
                    onItemClick = onItemClick,
                    onDismissed = { dismissedItems[item.library.id] = true },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SwipeableMediaItem(
    item: LibraryMedia,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = item.library.status.nextStatus != null,
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> onAdvance(item)
                SwipeToDismissBoxValue.EndToStart -> onRemove(item)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            if (direction != SwipeToDismissBoxValue.Settled) onDismissed()
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Move to next status",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove from library",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier,
    ) {
        MediaItemRow(
            item = item,
            onClick = { onItemClick(item) },
        )
    }
}

@Composable
private fun MediaItemRow(
    item: LibraryMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posterShape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.media.posterPath != null) {
            AsyncImage(
                model = TmdbImages.posterUrl(item.media.posterPath, "w154"),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp)
                    .clip(posterShape),
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = posterShape,
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.media.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.media.mediaType.name} · Added ${DateUtils.getRelativeTimeSpanString(item.library.addedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusListPreview() {
    CouchlistTheme {
        val sample = LibraryMedia(
            media = MediaItem(
                id = 1L,
                mediaType = MediaType.MOVIE,
                tmdbId = 550L,
                title = "Fight Club",
                originalTitle = "Fight Club",
                overview = null,
                posterPath = null,
                backdropPath = null,
                releaseDate = "1999-10-15",
                originalLanguage = "en",
                runtimeMinutes = 139,
                externalRating = 8.4,
                externalVoteCount = 0,
                genres = listOf("Drama"),
                lastRefreshedAt = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            library = LibraryItem(
                id = 1L,
                mediaId = 1L,
                status = MediaStatus.BACKLOG,
                progress = null,
                personalRating = null,
                favorite = false,
                notes = null,
                addedAt = 1L,
                startedAt = null,
                completedAt = null,
                updatedAt = 1L,
            ),
        )
        StatusList(
            status = MediaStatus.BACKLOG,
            items = listOf(sample),
            onAdvance = {},
            onRemove = {},
            onItemClick = {},
        )
    }
}
