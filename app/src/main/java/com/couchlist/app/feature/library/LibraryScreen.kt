package com.couchlist.app.feature.library

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.SmartFilter
import com.couchlist.app.core.domain.model.nextStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRoute(
    viewModel: LibraryViewModel = hiltViewModel(),
    onItemClick: (LibraryMedia) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showSmartFilterDialog by remember { mutableStateOf(false) }

    if (showCreateListDialog) {
        CreateListDialog(
            onDismiss = { showCreateListDialog = false },
            onConfirm = { name, description, type ->
                showCreateListDialog = false
                if (type == MediaListType.SMART_LIST) {
                    showSmartFilterDialog = true
                } else {
                    viewModel.onCreateList(name, description, type)
                }
            },
        )
    }

    if (showSmartFilterDialog) {
        SmartFilterDialog(
            onDismiss = { showSmartFilterDialog = false },
            onConfirm = { name, description, filter ->
                showSmartFilterDialog = false
                viewModel.onCreateSmartList(name, description, filter)
            },
        )
    }

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
                    is LibraryEvent.ShowMessage -> {
                        snackbarHostState.showSnackbar(event.message)
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
        onSortSelected = viewModel::onSortOptionSelected,
        onMultiSelectEnter = viewModel::onMultiSelectModeEnter,
        onMultiSelectExit = viewModel::onMultiSelectModeExit,
        onItemToggleSelection = viewModel::onItemToggleSelection,
        onSelectAll = viewModel::onSelectAll,
        onBatchMove = viewModel::onBatchMoveStatus,
        onBatchRemove = viewModel::onBatchRemove,
        onStatusTabChanged = viewModel::onStatusTabChanged,
        onCreateListClick = { showCreateListDialog = true },
        onDeleteList = viewModel::onDeleteList,
        onSmartListClick = viewModel::onSmartListClick,
        onSmartListBack = viewModel::onSmartListBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onMultiSelectEnter: () -> Unit,
    onMultiSelectExit: () -> Unit,
    onItemToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onBatchMove: () -> Unit,
    onBatchRemove: () -> Unit,
    onStatusTabChanged: (MediaStatus) -> Unit,
    onCreateListClick: () -> Unit,
    onDeleteList: (Long) -> Unit,
    onSmartListClick: (Long) -> Unit,
    onSmartListBack: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val statuses = MediaStatus.entries
    val selectedStatus = statuses[selectedTab]
    val statusItems = uiState.statusItems

    LaunchedEffect(selectedTab) {
        onStatusTabChanged(selectedStatus)
    }

    Scaffold(
        topBar = {
            if (uiState.selectedSmartListId != null) {
                TopAppBar(
                    title = {
                        Column {
                            val listSummary = uiState.lists.find { it.list.id == uiState.selectedSmartListId }
                            Text(text = listSummary?.list?.name ?: "Smart list")
                            uiState.selectedSmartListFilter?.let { filter ->
                                Text(
                                    text = filter.description(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onSmartListBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            } else if (uiState.isMultiSelectMode) {
                TopAppBar(
                    title = { Text(text = "${uiState.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = onMultiSelectExit) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Exit selection",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSelectAll) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Select all",
                            )
                        }
                        IconButton(onClick = onBatchRemove) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Remove selected",
                            )
                        }
                        if (selectedStatus.nextStatus != null) {
                            IconButton(onClick = onBatchMove) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Move selected",
                                )
                            }
                        }
                    },
                )
            } else {
                TopAppBar(title = { Text(text = "Library") })
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                ExtendedFloatingActionButton(
                    onClick = onCreateListClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New list") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.selectedSmartListId != null && uiState.selectedSmartListFilter != null) {
                val smartItems = uiState.smartListItems
                SortBar(
                    selectedOption = uiState.sortOption,
                    onOptionSelected = onSortSelected,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                SmartListContent(
                    items = smartItems,
                    onItemClick = onItemClick,
                    onItemLongClick = onMultiSelectEnter,
                    modifier = Modifier.weight(1f),
                )
            } else if (uiState.lists.isNotEmpty() && !uiState.isMultiSelectMode) {
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
                        ListSummaryCard(
                            summary = summary,
                            onDelete = { onDeleteList(summary.list.id) },
                            onClick = {
                                if (summary.list.type == MediaListType.SMART_LIST) {
                                    onSmartListClick(summary.list.id)
                                }
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            SortBar(
                selectedOption = uiState.sortOption,
                onOptionSelected = onSortSelected,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                isMultiSelectMode = uiState.isMultiSelectMode,
                selectedIds = uiState.selectedIds,
                onAdvance = onAdvance,
                onRemove = onRemove,
                onItemClick = onItemClick,
                onItemLongClick = onMultiSelectEnter,
                onItemToggleSelection = onItemToggleSelection,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SortBar(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortOption.entries.forEach { option ->
            FilterChip(
                selected = selectedOption == option,
                onClick = { onOptionSelected(option) },
                label = { Text(option.displayName) },
            )
        }
    }
}

@Composable
private fun ListSummaryCard(
    summary: MediaListSummary,
    onDelete: () -> Unit,
    onClick: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.width(172.dp)) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary.list.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (summary.list.type != MediaListType.PILE) Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Text(
                            text = "\u2022\u2022\u2022",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${summary.itemCount} ${if (summary.itemCount == 1) "title" else "titles"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary.list.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusList(
    status: MediaStatus,
    items: List<LibraryMedia>,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    onItemLongClick: () -> Unit,
    onItemToggleSelection: (Long) -> Unit,
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
                if (isMultiSelectMode) {
                    SelectableLibraryRow(
                        item = item,
                        isSelected = item.media.id in selectedIds,
                        onToggleSelection = { onItemToggleSelection(item.media.id) },
                        onItemClick = { onItemClick(item) },
                    )
                } else {
                    SwipeableLibraryRow(
                        item = item,
                        onAdvance = onAdvance,
                        onRemove = onRemove,
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick,
                        onDismissed = { dismissedItems[item.library.id] = true },
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SmartListContent(
    items: List<LibraryMedia>,
    onItemClick: (LibraryMedia) -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No matching titles",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Adjust your smart list filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items, key = { it.library.id }) { item ->
                LibraryMediaRow(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongClick = onItemLongClick,
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableLibraryRow(
    item: LibraryMedia,
    onAdvance: (LibraryMedia) -> Unit,
    onRemove: (LibraryMedia) -> Unit,
    onItemClick: (LibraryMedia) -> Unit,
    onItemLongClick: () -> Unit,
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
        LibraryMediaRow(
            item = item,
            onClick = { onItemClick(item) },
            onLongClick = onItemLongClick,
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryMediaRow(
    item: LibraryMedia,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.media.artworkUri != null) {
            AsyncImage(
                model = item.media.artworkUri,
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
                text = "${item.media.category.name} \u00B7 Added ${DateUtils.getRelativeTimeSpanString(item.library.addedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectableLibraryRow(
    item: LibraryMedia,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onItemClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickable { onToggleSelection() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() },
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (item.media.artworkUri != null) {
            AsyncImage(
                model = item.media.artworkUri,
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
                text = "${item.media.category.name} \u00B7 ${item.library.status.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CreateListDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, type: MediaListType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MediaListType.COLLECTION) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create new list") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "List type",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == MediaListType.TODO,
                        onClick = { selectedType = MediaListType.TODO },
                        label = { Text("TODO") },
                    )
                    FilterChip(
                        selected = selectedType == MediaListType.COLLECTION,
                        onClick = { selectedType = MediaListType.COLLECTION },
                        label = { Text("Collection") },
                    )
                    FilterChip(
                        selected = selectedType == MediaListType.SMART_LIST,
                        onClick = { selectedType = MediaListType.SMART_LIST },
                        label = { Text("Smart") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            description.trim().ifBlank { null },
                            selectedType,
                        )
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SmartFilterDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, filter: SmartFilter) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedStatuses by remember { mutableStateOf(setOf<MediaStatus>()) }
    var minRating by remember { mutableFloatStateOf(1f) }
    var useRatingFilter by remember { mutableStateOf(false) }
    var favoriteOnly by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create smart list") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Filter by status",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Leave empty to match all statuses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                MediaStatus.entries.forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = status in selectedStatuses,
                            onCheckedChange = { checked ->
                                selectedStatuses = if (checked) {
                                    selectedStatuses + status
                                } else {
                                    selectedStatuses - status
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = status.displayName)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = useRatingFilter,
                        onCheckedChange = { useRatingFilter = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Filter by rating")
                }
                if (useRatingFilter) {
                    Text(
                        text = "Minimum rating: ${minRating.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = minRating,
                        onValueChange = { minRating = it },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = favoriteOnly,
                        onCheckedChange = { favoriteOnly = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Favorites only")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val filter = SmartFilter(
                            statuses = selectedStatuses.takeIf { it.isNotEmpty() },
                            minRating = if (useRatingFilter) minRating.toInt() else null,
                            favoriteOnly = if (favoriteOnly) true else null,
                        )
                        onConfirm(
                            name.trim(),
                            description.trim().ifBlank { null },
                            filter,
                        )
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun emptyHint(status: MediaStatus): String = when (status) {
    MediaStatus.BACKLOG -> "Search for something worth saving."
    MediaStatus.WATCHING -> "Swipe a backlog title right when you start it."
    MediaStatus.COMPLETED -> "Finished titles will collect here."
    MediaStatus.ABANDONED -> "Titles you stop watching can rest here."
}
