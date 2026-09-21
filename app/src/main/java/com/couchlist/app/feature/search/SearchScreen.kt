package com.couchlist.app.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
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
import com.couchlist.app.core.ui.theme.CouchlistTheme
import coil3.compose.AsyncImage

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
        onAddToWatchlist = viewModel::onAddToWatchlist,
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
    onAddToWatchlist: (MediaSearchResult) -> Unit,
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
            when {
                uiState.query.isBlank() -> SearchEmptyState()
                uiState.errorMessage != null -> SearchErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                )
                else -> SearchResultsGrid(
                    results = uiState.results,
                    inLibraryReferences = uiState.inLibraryReferences,
                    onAddToWatchlist = onAddToWatchlist,
                    onResultClick = onResultClick,
                )
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester),
    )
}

@Composable
private fun SearchEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Search movies and TV shows to add to your lists.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) {
            Text(text = "Try again")
        }
    }
}

@Composable
private fun SearchResultsGrid(
    results: List<MediaSearchResult>,
    inLibraryReferences: Set<MediaReference>,
    onAddToWatchlist: (MediaSearchResult) -> Unit,
    onResultClick: (MediaSearchResult) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = results, key = { it.reference.stableKey }) { result ->
            MediaResultCard(
                result = result,
                isInLibrary = result.reference in inLibraryReferences,
                onAddToWatchlist = onAddToWatchlist,
                onResultClick = onResultClick,
            )
        }
    }
}

@Composable
private fun MediaResultCard(
    result: MediaSearchResult,
    isInLibrary: Boolean,
    onAddToWatchlist: (MediaSearchResult) -> Unit,
    onResultClick: (MediaSearchResult) -> Unit,
) {
    Column(modifier = Modifier.clickable { onResultClick(result) }) {
        Box {
            AsyncImage(
                model = result.artworkUri,
                contentDescription = result.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            ) {
                Text(
                    text = result.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (isInLibrary) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "In library",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = { onAddToWatchlist(result) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add ${result.title} to Watchlist",
                    )
                }
            }
        }
        Text(
            text = result.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val subtitle = listOfNotNull(
            result.releaseYear?.toString(),
            if (result.voteAverage > 0.0) "${result.voteAverage}" else null,
        ).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (result.voteAverage > 0.0) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
            onAddToWatchlist = {},
            onResultClick = {},
        )
    }
}
