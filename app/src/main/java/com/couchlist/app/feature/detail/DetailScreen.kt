package com.couchlist.app.feature.detail

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.ProviderCategory
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.ui.components.TmdbImages
import com.couchlist.app.core.ui.theme.CouchlistTheme
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailRoute(
    viewModel: DetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is DetailEvent.ShowMessage ->
                        snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.detail?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                uiState.errorMessage != null -> DetailErrorState(
                    message = uiState.errorMessage,
                    onRetry = viewModel::onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                uiState.detail != null -> DetailContent(
                    uiState = uiState,
                    onAddToWatchlist = viewModel::onAddToWatchlist,
                    onSetStatus = viewModel::onSetStatus,
                    onRemoveFromWatchlist = viewModel::onRemoveFromWatchlist,
                )

                else -> DetailErrorState(
                    message = "Couldn't load this title.",
                    onRetry = viewModel::onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    uiState: DetailUiState,
    onAddToWatchlist: () -> Unit,
    onSetStatus: (MediaStatus) -> Unit,
    onRemoveFromWatchlist: () -> Unit,
) {
    val detail = uiState.detail ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (uiState.isOffline) {
            OfflineBanner()
        }
        AsyncImage(
            model = TmdbImages.backdropUrl(detail.backdropPath)
                ?: TmdbImages.posterUrl(detail.posterPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            val subtitleParts = buildList {
                detail.releaseYear?.let { add(it.toString()) }
                add(detail.mediaType.name)
            }.joinToString(" · ")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitleParts,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (detail.voteAverage > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rating ${detail.voteAverage} / 10",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (!detail.overview.isNullOrBlank()) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.overview,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (uiState.status == null) "Add to your lists" else "Status",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.status == null) {
                Button(
                    onClick = onAddToWatchlist,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Add to Watchlist")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaStatus.entries.forEach { status ->
                        FilterChip(
                            selected = uiState.status == status,
                            onClick = { onSetStatus(status) },
                            label = { Text(text = status.displayName) },
                        )
                    }
                }
                TextButton(onClick = onRemoveFromWatchlist) {
                    Text(text = "Remove from lists")
                }
            }
            if (detail.providers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Where to watch",
                    style = MaterialTheme.typography.titleMedium,
                )
                detail.providers.groupBy { it.category }.forEach { (category, providers) ->
                    ProviderRow(label = category.displayName, providers = providers)
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "You're offline: showing the details you saved",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProviderRow(
    label: String,
    providers: List<WatchProvider>,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items = providers, key = { it.providerId }) { provider ->
                Column(
                    modifier = Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = TmdbImages.logoUrl(provider.logoPath),
                        contentDescription = provider.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(text = "Try again")
        }
    }
}

private val ProviderCategory.displayName: String
    get() = when (this) {
        ProviderCategory.FLATRATE -> "Stream"
        ProviderCategory.RENT -> "Rent"
        ProviderCategory.BUY -> "Buy"
    }

@Preview(showBackground = true)
@Composable
private fun DetailContentPreview() {
    CouchlistTheme {
        DetailContent(
            uiState = DetailUiState(
                detail = MediaDetail(
                    id = 550,
                    mediaType = MediaType.MOVIE,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    overview = "A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy.",
                    releaseDate = "1999-10-15",
                    originalLanguage = "en",
                    runtimeMinutes = 139,
                    posterPath = null,
                    backdropPath = null,
                    voteAverage = 8.4,
                    voteCount = 30_000,
                    genres = listOf("Drama"),
                    providers = listOf(
                        WatchProvider(
                            providerId = 8,
                            name = "Netflix",
                            logoPath = null,
                            category = ProviderCategory.FLATRATE,
                        ),
                    ),
                ),
                status = MediaStatus.BACKLOG,
                entryId = 1L,
            ),
            onAddToWatchlist = {},
            onSetStatus = {},
            onRemoveFromWatchlist = {},
        )
    }
}
