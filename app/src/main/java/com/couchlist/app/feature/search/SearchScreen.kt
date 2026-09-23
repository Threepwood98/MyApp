package com.couchlist.app.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.couchlist.app.core.domain.model.MediaCategory
import com.couchlist.app.core.domain.model.MediaReference
import com.couchlist.app.core.domain.model.MediaSearchResult
import com.couchlist.app.core.ui.components.EmptyState
import com.couchlist.app.core.ui.components.ErrorState
import com.couchlist.app.core.ui.components.LibraryItemCard
import com.couchlist.app.core.ui.components.LoadingState
import com.couchlist.app.core.ui.theme.CouchlistTheme
import com.couchlist.app.core.ui.theme.spacing

@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onDetailClick: (Long) -> Unit = {},
    showBack: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is SearchEvent.ShowMessage ->
                        snackbarHostState.showSnackbar(event.message)
                    is SearchEvent.NavigateToDetail -> onDetailClick(event.mediaId)
                }
            }
        }
    }

    SearchContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        showBack = showBack,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::onRetry,
        onAddToPile = viewModel::onAddToPile,
        onResultClick = viewModel::onResultClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(
    uiState: SearchUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    showBack: Boolean,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onAddToPile: (MediaSearchResult) -> Unit,
    onResultClick: (MediaSearchResult) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Search") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
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
            SearchField(
                query = uiState.query,
                isSearching = uiState.isSearching,
                onQueryChange = onQueryChange,
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    uiState.query.isBlank() -> EmptyState(
                        title = "Find your next favorite",
                        message = "Search movies and TV shows to add to your lists.",
                        modifier = Modifier.fillMaxSize(),
                    )
                    uiState.errorMessage != null -> ErrorState(
                        message = uiState.errorMessage,
                        modifier = Modifier.fillMaxSize(),
                        action = {
                            Button(onClick = onRetry) {
                                Text(text = "Try again")
                            }
                        },
                    )
                    uiState.isSearching && uiState.results.isEmpty() -> LoadingState(
                        label = "Searching",
                        modifier = Modifier.fillMaxSize(),
                    )
                    uiState.results.isEmpty() -> EmptyState(
                        title = "No matches found",
                        message = "Try a different title or spelling.",
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> SearchResultsGrid(
                        results = uiState.results,
                        inLibraryReferences = uiState.inLibraryReferences,
                        inPileReferences = uiState.inPileReferences,
                        onAddToPile = onAddToPile,
                        onResultClick = onResultClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(text = "Search movies & TV") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.small,
            )
            .focusRequester(focusRequester),
    )
}

@Composable
private fun SearchResultsGrid(
    results: List<MediaSearchResult>,
    inLibraryReferences: Set<MediaReference>,
    inPileReferences: Set<MediaReference>,
    onAddToPile: (MediaSearchResult) -> Unit,
    onResultClick: (MediaSearchResult) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(144.dp),
        contentPadding = PaddingValues(MaterialTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = results, key = { it.reference.stableKey }) { result ->
            MediaResultCard(
                result = result,
                isInLibrary = result.reference in inLibraryReferences,
                isInPile = result.reference in inPileReferences,
                onAddToPile = onAddToPile,
                onResultClick = onResultClick,
            )
        }
    }
}

@Composable
private fun MediaResultCard(
    result: MediaSearchResult,
    isInLibrary: Boolean,
    isInPile: Boolean,
    onAddToPile: (MediaSearchResult) -> Unit,
    onResultClick: (MediaSearchResult) -> Unit,
) {
    val subtitle = listOfNotNull(
        result.releaseYear?.toString(),
        result.voteAverage.takeIf { it > 0.0 }?.toString(),
    ).joinToString(" · ")
    LibraryItemCard(
        title = result.title,
        subtitle = subtitle.takeIf { it.isNotBlank() },
        artworkModel = result.artworkUri,
        onClick = { onResultClick(result) },
        artworkOverlay = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(MaterialTheme.spacing.small),
            ) {
                Text(
                    text = result.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.extraSmall,
                    ),
                )
            }
            if (isInLibrary || isInPile) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(MaterialTheme.spacing.small),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.small,
                            vertical = MaterialTheme.spacing.extraSmall,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(
                            text = if (isInPile) "In Pile" else "In library",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            if (!isInPile) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MaterialTheme.spacing.small),
                ) {
                    IconButton(onClick = { onAddToPile(result) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add ${result.title} to The Pile",
                        )
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchResultsPreview() {
    CouchlistTheme {
        SearchContent(
            uiState = SearchUiState(
                query = "fight club",
                results = listOf(
                    MediaSearchResult(
                        reference = MediaReference("tmdb", MediaCategory.MOVIE, "550"),
                        title = "Fight Club",
                        artworkUri = null,
                        releaseYear = 1999,
                        description = "A ticking-time-bomb insomniac...",
                    ),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            showBack = true,
            onQueryChange = {},
            onRetry = {},
            onAddToPile = {},
            onResultClick = {},
        )
    }
}
