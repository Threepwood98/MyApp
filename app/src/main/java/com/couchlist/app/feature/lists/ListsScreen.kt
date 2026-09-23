package com.couchlist.app.feature.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.couchlist.app.core.domain.model.ListGroup
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.ui.components.EmptyState
import com.couchlist.app.core.ui.components.ErrorState
import com.couchlist.app.core.ui.components.LibraryItemCard
import com.couchlist.app.core.ui.components.ListCard as CouchlistListCard
import com.couchlist.app.core.ui.components.LoadingState
import com.couchlist.app.core.ui.components.MediaPoster
import com.couchlist.app.core.ui.components.MediaProgressBadge
import com.couchlist.app.core.ui.components.SectionHeader
import com.couchlist.app.core.ui.theme.dimensions
import com.couchlist.app.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsRoute(
    onListClick: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showAddSheet by remember { mutableStateOf(false) }
    var showCreateList by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<MediaList?>(null) }
    var groupingList by remember { mutableStateOf<MediaList?>(null) }
    var deletingList by remember { mutableStateOf<MediaList?>(null) }
    var editingGroup by remember { mutableStateOf<ListGroup?>(null) }
    var deletingGroup by remember { mutableStateOf<ListGroup?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is ListsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lists") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More destinations")
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Library") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onLibraryClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Statistics") },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    onStatisticsClick()
                                },
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            uiState.isLoading -> LoadingState(modifier = contentModifier.fillMaxSize())
            uiState.errorMessage != null -> ErrorState(
                message = uiState.errorMessage.orEmpty(),
                modifier = contentModifier.fillMaxSize(),
            )
            uiState.pile == null && uiState.allLists.isEmpty() -> EmptyState(
                title = "Your library is ready",
                message = "Create a list or save something interesting to get started.",
                modifier = contentModifier.fillMaxSize(),
            )
            else -> ListsContent(
                state = uiState,
                onListClick = onListClick,
                onItemClick = onItemClick,
                onEditList = { editingList = it },
                onDeleteList = { deletingList = it },
                onDuplicateList = viewModel::duplicateList,
                onTogglePinned = viewModel::togglePinned,
                onSetGroup = { groupingList = it },
                onMoveList = viewModel::moveList,
                onEditGroup = { editingGroup = it },
                onDeleteGroup = { deletingGroup = it },
                onMoveGroup = viewModel::moveGroup,
                modifier = contentModifier,
            )
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Text(
                text = "Add to Couchlist",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Search media") },
                supportingContent = { Text("Find a movie or show and save it to The Pile") },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    onSearchClick()
                },
            )
            ListItem(
                headlineContent = { Text("Create list") },
                supportingContent = { Text("Make a to-do list or collection") },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    showCreateList = true
                },
            )
            ListItem(
                headlineContent = { Text("Create group") },
                supportingContent = { Text("Organize related lists together") },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    showCreateGroup = true
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCreateList) {
        ListEditorDialog(
            title = "Create list",
            groups = uiState.groups.map { it.group },
            onDismiss = { showCreateList = false },
            onConfirm = { name, description, type, groupId ->
                viewModel.createList(name, description, type, groupId)
                showCreateList = false
            },
        )
    }
    editingList?.let { list ->
        ListEditorDialog(
            title = "Edit list",
            initial = list,
            groups = uiState.groups.map { it.group },
            onDismiss = { editingList = null },
            onConfirm = { name, description, type, groupId ->
                viewModel.updateList(list, name, description, type, groupId)
                editingList = null
            },
        )
    }
    groupingList?.let { list ->
        GroupPickerDialog(
            groups = uiState.groups.map { it.group },
            selectedGroupId = list.groupId,
            onDismiss = { groupingList = null },
            onSelect = { groupId ->
                viewModel.setListGroup(list.id, groupId)
                groupingList = null
            },
        )
    }
    deletingList?.let { list ->
        ConfirmDeleteDialog(
            name = list.name,
            onDismiss = { deletingList = null },
            onConfirm = { viewModel.deleteList(list); deletingList = null },
        )
    }
    if (showCreateGroup) {
        NameDialog(
            title = "Create group",
            confirmLabel = "Create",
            onDismiss = { showCreateGroup = false },
            onConfirm = { viewModel.createGroup(it); showCreateGroup = false },
        )
    }
    editingGroup?.let { group ->
        NameDialog(
            title = "Rename group",
            initialName = group.name,
            confirmLabel = "Save",
            onDismiss = { editingGroup = null },
            onConfirm = { viewModel.renameGroup(group, it); editingGroup = null },
        )
    }
    deletingGroup?.let { group ->
        ConfirmDeleteDialog(
            name = group.name,
            supportingText = "Lists in this group will become ungrouped.",
            onDismiss = { deletingGroup = null },
            onConfirm = { viewModel.deleteGroup(group); deletingGroup = null },
        )
    }
}

@Composable
private fun ListsContent(
    state: ListsUiState,
    onListClick: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onEditList: (MediaList) -> Unit,
    onDeleteList: (MediaList) -> Unit,
    onDuplicateList: (MediaList) -> Unit,
    onTogglePinned: (MediaList) -> Unit,
    onSetGroup: (MediaList) -> Unit,
    onMoveList: (MediaList, Int) -> Unit,
    onEditGroup: (ListGroup) -> Unit,
    onDeleteGroup: (ListGroup) -> Unit,
    onMoveGroup: (ListGroup, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Everything worth returning to.", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Save first, then shape your own shelves.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.pile?.let { pile ->
            item(key = "pile") {
                PileCard(pile, state.pileItems, onClick = { onListClick(pile.list.id) })
            }
        }
        if (state.enjoyingItems.isNotEmpty()) {
            item(key = "enjoying") {
                EnjoyingShelf(state.enjoyingItems, onItemClick)
            }
        }
        if (state.pinnedLists.isNotEmpty()) {
            item(key = "pinned") {
                ListShelf(
                    title = "Pinned",
                    lists = state.pinnedLists,
                    onListClick = onListClick,
                    onEditList = onEditList,
                    onDeleteList = onDeleteList,
                    onDuplicateList = onDuplicateList,
                    onTogglePinned = onTogglePinned,
                    onSetGroup = onSetGroup,
                    onMoveList = onMoveList,
                )
            }
        }
        items(state.groups, key = { "group-${it.group.id}" }) { section ->
            GroupShelf(
                section = section,
                onListClick = onListClick,
                onEditList = onEditList,
                onDeleteList = onDeleteList,
                onDuplicateList = onDuplicateList,
                onTogglePinned = onTogglePinned,
                onSetGroup = onSetGroup,
                onMoveList = onMoveList,
                onEditGroup = onEditGroup,
                onDeleteGroup = onDeleteGroup,
                onMoveGroup = onMoveGroup,
            )
        }
        if (state.ungroupedLists.isNotEmpty()) {
            item(key = "my-lists") {
                ListShelf(
                    title = "My lists",
                    lists = state.ungroupedLists,
                    onListClick = onListClick,
                    onEditList = onEditList,
                    onDeleteList = onDeleteList,
                    onDuplicateList = onDuplicateList,
                    onTogglePinned = onTogglePinned,
                    onSetGroup = onSetGroup,
                    onMoveList = onMoveList,
                )
            }
        }
    }
}

@Composable
private fun EnjoyingShelf(items: List<LibraryMedia>, onItemClick: (Long) -> Unit) {
    Column {
        SectionHeader(
            title = "Enjoying",
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.library.id }) { item ->
                LibraryItemCard(
                    title = item.media.title,
                    subtitle = item.tracking?.mode?.displayName,
                    artworkModel = item.media.artworkUri,
                    onClick = { onItemClick(item.media.id) },
                    modifier = Modifier.width(MaterialTheme.dimensions.posterMediumWidth),
                    artworkOverlay = item.progress?.let { progress ->
                        {
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
        }
    }
}

@Composable
private fun PileCard(summary: MediaListSummary, items: List<LibraryMedia>, onClick: () -> Unit) {
    Column {
        SectionHeader(
            title = "The Pile",
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clickable(onClick = onClick),
        ) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Your inbox", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (summary.itemCount == 0) "Drop interesting finds here and organize them later."
                            else "${summary.itemCount} waiting to be organized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = onClick) { Text("Open") }
                }
                if (items.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items.take(8), key = { it.library.id }) { item ->
                            PosterThumb(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterThumb(item: LibraryMedia) {
    MediaPoster(
        model = item.media.artworkUri,
        contentDescription = null,
        modifier = Modifier.width(MaterialTheme.dimensions.posterSmallWidth),
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun GroupShelf(
    section: ListGroupSection,
    onListClick: (Long) -> Unit,
    onEditList: (MediaList) -> Unit,
    onDeleteList: (MediaList) -> Unit,
    onDuplicateList: (MediaList) -> Unit,
    onTogglePinned: (MediaList) -> Unit,
    onSetGroup: (MediaList) -> Unit,
    onMoveList: (MediaList, Int) -> Unit,
    onEditGroup: (ListGroup) -> Unit,
    onDeleteGroup: (ListGroup) -> Unit,
    onMoveGroup: (ListGroup, Int) -> Unit,
) {
    Column {
        SectionHeader(
            title = section.group.name,
            modifier = Modifier.padding(start = MaterialTheme.spacing.large, end = MaterialTheme.spacing.small),
            action = { GroupMenu(section.group, onEditGroup, onDeleteGroup, onMoveGroup) },
        )
        if (section.lists.isEmpty()) {
            Text(
                "No lists in this group yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            ListShelfContent(
                lists = section.lists,
                onListClick = onListClick,
                onEditList = onEditList,
                onDeleteList = onDeleteList,
                onDuplicateList = onDuplicateList,
                onTogglePinned = onTogglePinned,
                onSetGroup = onSetGroup,
                onMoveList = onMoveList,
            )
        }
    }
}

@Composable
private fun ListShelf(
    title: String,
    lists: List<MediaListSummary>,
    onListClick: (Long) -> Unit,
    onEditList: (MediaList) -> Unit,
    onDeleteList: (MediaList) -> Unit,
    onDuplicateList: (MediaList) -> Unit,
    onTogglePinned: (MediaList) -> Unit,
    onSetGroup: (MediaList) -> Unit,
    onMoveList: (MediaList, Int) -> Unit,
) {
    Column {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        ListShelfContent(
            lists,
            onListClick,
            onEditList,
            onDeleteList,
            onDuplicateList,
            onTogglePinned,
            onSetGroup,
            onMoveList,
        )
    }
}

@Composable
private fun ListShelfContent(
    lists: List<MediaListSummary>,
    onListClick: (Long) -> Unit,
    onEditList: (MediaList) -> Unit,
    onDeleteList: (MediaList) -> Unit,
    onDuplicateList: (MediaList) -> Unit,
    onTogglePinned: (MediaList) -> Unit,
    onSetGroup: (MediaList) -> Unit,
    onMoveList: (MediaList, Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lists, key = { it.list.id }) { summary ->
            ListCard(
                summary,
                onClick = { onListClick(summary.list.id) },
                onEdit = { onEditList(summary.list) },
                onDelete = { onDeleteList(summary.list) },
                onDuplicate = { onDuplicateList(summary.list) },
                onTogglePinned = { onTogglePinned(summary.list) },
                onSetGroup = { onSetGroup(summary.list) },
                onMoveUp = { onMoveList(summary.list, -1) },
                onMoveDown = { onMoveList(summary.list, 1) },
            )
        }
    }
}

@Composable
private fun ListCard(
    summary: MediaListSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePinned: () -> Unit,
    onSetGroup: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    CouchlistListCard(
        title = summary.list.name,
        supportingText = "${summary.itemCount} ${if (summary.itemCount == 1) "item" else "items"} · ${summary.list.type.label}",
        artworkModel = summary.coverArtworkUri,
        onClick = onClick,
        modifier = Modifier.width(168.dp),
        artworkOverlay = {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Actions for ${summary.list.name}",
                        )
                    }
                }
                DropdownMenu(menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    fun dismiss(action: () -> Unit) { menuExpanded = false; action() }
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { dismiss(onEdit) })
                    DropdownMenuItem(
                        text = { Text(if (summary.list.isPinned) "Unpin" else "Pin") },
                        onClick = { dismiss(onTogglePinned) },
                    )
                    DropdownMenuItem(text = { Text("Move to group") }, onClick = { dismiss(onSetGroup) })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { dismiss(onDuplicate) })
                    DropdownMenuItem(text = { Text("Move earlier") }, onClick = { dismiss(onMoveUp) })
                    DropdownMenuItem(text = { Text("Move later") }, onClick = { dismiss(onMoveDown) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { dismiss(onDelete) })
                }
            }
        },
    )
}

@Composable
private fun GroupMenu(
    group: ListGroup,
    onEdit: (ListGroup) -> Unit,
    onDelete: (ListGroup) -> Unit,
    onMove: (ListGroup, Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Group actions")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onEdit(group) })
            DropdownMenuItem(text = { Text("Move earlier") }, onClick = { expanded = false; onMove(group, -1) })
            DropdownMenuItem(text = { Text("Move later") }, onClick = { expanded = false; onMove(group, 1) })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete(group) })
        }
    }
}

@Composable
private fun ListEditorDialog(
    title: String,
    groups: List<ListGroup>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, MediaListType, Long?) -> Unit,
    initial: MediaList? = null,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var type by remember(initial) { mutableStateOf(initial?.type ?: MediaListType.COLLECTION) }
    var groupId by remember(initial) { mutableStateOf(initial?.groupId) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    val typeEditable = initial?.type != MediaListType.SMART_LIST
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, maxLines = 3)
                if (typeEditable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(type == MediaListType.TODO, { type = MediaListType.TODO }, label = { Text("To do") })
                        FilterChip(type == MediaListType.COLLECTION, { type = MediaListType.COLLECTION }, label = { Text("Collection") })
                    }
                }
                Box {
                    TextButton(onClick = { groupMenuExpanded = true }) {
                        Text(groups.firstOrNull { it.id == groupId }?.name ?: "Ungrouped")
                    }
                    DropdownMenu(groupMenuExpanded, onDismissRequest = { groupMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Ungrouped") }, onClick = { groupId = null; groupMenuExpanded = false })
                        groups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = { groupId = group.id; groupMenuExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, description.takeIf { it.isNotBlank() }, type, groupId) },
            ) { Text(if (initial == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GroupPickerDialog(
    groups: List<ListGroup>,
    selectedGroupId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to group") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Ungrouped") },
                    trailingContent = { if (selectedGroupId == null) Text("Selected") },
                    modifier = Modifier.clickable { onSelect(null) },
                )
                groups.forEach { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        trailingContent = { if (selectedGroupId == group.id) Text("Selected") },
                        modifier = Modifier.clickable { onSelect(group.id) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialName: String = "",
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmDeleteDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    supportingText: String = "Items remain in your Library.",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $name?") },
        text = { Text(supportingText) },
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val MediaListType.label: String
    get() = when (this) {
        MediaListType.PILE -> "Pile"
        MediaListType.TODO -> "To do"
        MediaListType.COLLECTION -> "Collection"
        MediaListType.SMART_LIST -> "Smart"
    }
