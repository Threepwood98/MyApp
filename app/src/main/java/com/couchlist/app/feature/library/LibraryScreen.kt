package com.couchlist.app.feature.library

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.nextStatus
import com.couchlist.app.core.ui.components.TmdbImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRoute(
    viewModel: LibraryViewModel = hiltViewModel(),
    onItemClick: (LibraryMedia) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is LibraryEvent.ShowUndo -> {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = "Undo",
                            withDismissAction = true,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onUndo(event.action)
                        }
                    }
                }
            }
        }
    }

    LibraryContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAdvance = viewModel::onAdvanceStatus,
        onRemove = viewModel::onRemove,
        onItemClick = onItemClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val statuses = MediaStatus.entries
    val selectedStatus = statuses[selectedTab]
    val statusItems = uiState.items.filter { it.library.status == selectedStatus }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Library") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.lists.isNotEmpty()) {
                Text(
                    text = "Your lists",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics { heading() },
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.lists, key = { it.list.id }) { summary ->
                        ListSummaryCard(summary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                statuses.forEachIndexed { index, status ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = status.displayName) },
                    )
                }
            }
            StatusList(
                status = selectedStatus,
                items = statusItems,
                onAdvance = onAdvance,
                onRemove = onRemove,
                onItemClick = onItemClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ListSummaryCard(summary: MediaListSummary) {
    ElevatedCard(modifier = Modifier.width(172.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = summary.list.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${summary.itemCount} ${if (summary.itemCount == 1) "title" else "titles"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No ${status.displayName.lowercase()} titles",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = emptyHint(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(visibleItems, key = { it.library.id }) { item ->
                SwipeableLibraryRow(
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
private fun SwipeableLibraryRow(
    item: LibraryMedia,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    onDismissed: () -> Unit,
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
        backgroundContent = { SwipeBackground() },
    ) {
        LibraryMediaRow(item = item, onClick = { onItemClick(item) })
    }
}

@Composable
private fun SwipeBackground() {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Move to next status",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove from library",
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun LibraryMediaRow(
    item: LibraryMedia,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
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
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
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
            Text(
                text = "${item.media.mediaType.name} · Added ${DateUtils.getRelativeTimeSpanString(item.library.addedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun emptyHint(status: MediaStatus): String = when (status) {
    MediaStatus.BACKLOG -> "Search for something worth saving."
    MediaStatus.WATCHING -> "Swipe a backlog title right when you start it."
    MediaStatus.COMPLETED -> "Finished titles will collect here."
    MediaStatus.ABANDONED -> "Titles you stop watching can rest here."
}
