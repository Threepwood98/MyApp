package com.couchlist.app.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.ui.components.EmptyState
import com.couchlist.app.core.ui.components.ErrorState
import com.couchlist.app.core.ui.components.LibraryItemCard
import com.couchlist.app.core.ui.components.LoadingState
import com.couchlist.app.core.ui.components.MediaProgressBadge
import com.couchlist.app.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailRoute(
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: ListDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var selectedItem by remember { mutableStateOf<LibraryMedia?>(null) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is ListDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.list?.name ?: "List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                label = "Loading list",
            )
            state.list == null -> ErrorState(
                title = "List unavailable",
                message = "This list no longer exists.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> ListDetailContent(
                state = state,
                onToggleCompleted = viewModel::toggleCompletedVisibility,
                onItemClick = { onItemClick(it.media.id) },
                onItemActions = { selectedItem = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    selectedItem?.let { item ->
        ItemActionsSheet(
            item = item,
            sourceType = state.list?.type,
            targets = state.targetLists,
            onDismiss = { selectedItem = null },
            onRemove = { viewModel.remove(item); selectedItem = null },
            onCopy = { targetId -> viewModel.copy(item, targetId); selectedItem = null },
            onMove = { targetId -> viewModel.move(item, targetId); selectedItem = null },
            onStartTracking = { viewModel.startTracking(item); selectedItem = null },
            onPauseTracking = { viewModel.pauseTracking(item); selectedItem = null },
            onResumeTracking = { viewModel.resumeTracking(item); selectedItem = null },
            onCompleteTracking = { viewModel.completeTracking(item); selectedItem = null },
            onAbandonTracking = { viewModel.abandonTracking(item); selectedItem = null },
        )
    }
}

@Composable
private fun ListDetailContent(
    state: ListDetailUiState,
    onToggleCompleted: () -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    onItemActions: (LibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        state.list?.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (state.list?.type == MediaListType.TODO) {
            FilterChip(
                selected = state.showCompleted,
                onClick = onToggleCompleted,
                label = { Text("Show completed") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (state.visibleItems.isEmpty()) {
            EmptyState(
                title = if (state.list?.type == MediaListType.PILE) "The Pile is clear" else "Nothing here yet",
                message = if (state.list?.type == MediaListType.PILE) {
                    "Use Search to save something interesting."
                } else {
                    "Add media from its detail page."
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(144.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.visibleItems, key = { it.library.id }) { item ->
                    ListMediaCard(
                        item,
                        showActions = state.list?.type != MediaListType.SMART_LIST,
                        onClick = { onItemClick(item) },
                        onActions = { onItemActions(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ListMediaCard(
    item: LibraryMedia,
    showActions: Boolean,
    onClick: () -> Unit,
    onActions: () -> Unit,
) {
    LibraryItemCard(
        title = item.media.title,
        subtitle = item.status.displayName,
        artworkModel = item.media.artworkUri,
        onClick = onClick,
        artworkOverlay = {
            if (showActions) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.spacing.small),
                ) {
                    IconButton(onClick = onActions) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Actions for ${item.media.title}")
                    }
                }
            }
            item.progress?.let { progress ->
                MediaProgressBadge(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MaterialTheme.spacing.small),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemActionsSheet(
    item: LibraryMedia,
    sourceType: MediaListType?,
    targets: List<MediaListSummary>,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onCopy: (Long) -> Unit,
    onMove: (Long) -> Unit,
    onStartTracking: () -> Unit,
    onPauseTracking: () -> Unit,
    onResumeTracking: () -> Unit,
    onCompleteTracking: () -> Unit,
    onAbandonTracking: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(item.media.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
        TextButton(onClick = onRemove, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(if (sourceType == MediaListType.PILE) "Remove from The Pile" else "Remove from list")
        }
        Text(
            "Tracking",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        when (item.tracking?.state) {
            null, TrackingState.COMPLETED, TrackingState.ABANDONED ->
                TextButton(onClick = onStartTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(if (item.tracking == null) "Start tracking" else "Start again")
                }
            TrackingState.ACTIVE -> {
                TextButton(onClick = onPauseTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Pause")
                }
                TextButton(onClick = onCompleteTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Complete")
                }
                TextButton(onClick = onAbandonTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Abandon")
                }
            }
            TrackingState.PAUSED -> {
                TextButton(onClick = onResumeTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Resume")
                }
                TextButton(onClick = onCompleteTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Complete")
                }
                TextButton(onClick = onAbandonTracking, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Abandon")
                }
            }
        }
        if (targets.isNotEmpty()) {
            Text(
                "Organize",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            targets.forEach { target ->
                ListItem(
                    headlineContent = { Text(target.list.name) },
                    supportingContent = { Text("${target.itemCount} items") },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { onCopy(target.list.id) }) { Text("Copy") }
                            TextButton(onClick = { onMove(target.list.id) }) { Text("Move") }
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
