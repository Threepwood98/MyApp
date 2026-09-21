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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaStatus
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.ProviderCategory
import com.couchlist.app.core.domain.model.TvEpisode
import com.couchlist.app.core.domain.model.TvSeason
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
    var showRatingDialog by remember { mutableStateOf(false) }
    var showEditRatingDialog by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }

    if (showRatingDialog) {
        RatingNotesDialog(
            onDismiss = { showRatingDialog = false },
            onConfirm = { rating, notes ->
                showRatingDialog = false
                viewModel.onMarkWatched(rating, notes)
            },
        )
    }

    if (showEditRatingDialog) {
        EditRatingDialog(
            currentRating = uiState.personalRating,
            onDismiss = { showEditRatingDialog = false },
            onConfirm = { rating ->
                showEditRatingDialog = false
                viewModel.onSetRating(rating)
            },
        )
    }

    if (showEditNotesDialog) {
        EditNotesDialog(
            currentNotes = uiState.notes,
            onDismiss = { showEditNotesDialog = false },
            onConfirm = { notes ->
                showEditNotesDialog = false
                viewModel.onSetNotes(notes)
            },
        )
    }

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
                actions = {
                    if (uiState.status != null) {
                        IconButton(onClick = viewModel::onToggleFavorite) {
                            Icon(
                                imageVector = if (uiState.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (uiState.favorite) "Remove from favorites" else "Add to favorites",
                                tint = if (uiState.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    onMarkAsWatchedClick = { showRatingDialog = true },
                    onEditRating = { showEditRatingDialog = true },
                    onEditNotes = { showEditNotesDialog = true },
                    onRewatch = viewModel::onRewatch,
                    onSeasonSelected = viewModel::onSeasonSelected,
                    onEpisodeWatchedChange = viewModel::onEpisodeWatchedChange,
                    onRetrySeason = viewModel::onRetrySeason,
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
    onMarkAsWatchedClick: () -> Unit,
    onEditRating: () -> Unit,
    onEditNotes: () -> Unit,
    onRewatch: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeWatchedChange: (TvEpisode, Boolean) -> Unit,
    onRetrySeason: () -> Unit,
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
            if (uiState.status != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PersonalRatingRow(
                    rating = uiState.personalRating,
                    onClick = onEditRating,
                )
                Spacer(modifier = Modifier.height(8.dp))
                NotesRow(
                    notes = uiState.notes,
                    onClick = onEditNotes,
                )
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MediaStatus.entries) { status ->
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
            if (detail.mediaType == MediaType.MOVIE) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onMarkAsWatchedClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Mark as watched")
                }
                if (uiState.status == MediaStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRewatch,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Log rewatch")
                    }
                }
            }
            if (detail.mediaType == MediaType.TV && uiState.seasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                uiState.nextUnwatchedEpisode?.let { episode ->
                    NextUnwatchedBanner(episode = episode)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                EpisodeTrackingSection(
                    uiState = uiState,
                    onSeasonSelected = onSeasonSelected,
                    onEpisodeWatchedChange = onEpisodeWatchedChange,
                    onRetry = onRetrySeason,
                )
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
private fun EpisodeTrackingSection(
    uiState: DetailUiState,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeWatchedChange: (TvEpisode, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val selectedSeason = uiState.seasons.firstOrNull {
        it.seasonNumber == uiState.selectedSeasonNumber
    }
    val progress = uiState.tvProgress

    Text(text = "Episodes", style = MaterialTheme.typography.titleLarge)
    if (progress.totalEpisodes > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${progress.watchedEpisodes} of ${progress.totalEpisodes} episodes watched",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.fraction?.toFloat() ?: 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (uiState.entryId == null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add this show to your Watchlist to track watched episodes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.seasons, key = { it.id }) { season ->
            FilterChip(
                selected = season.seasonNumber == uiState.selectedSeasonNumber,
                onClick = { onSeasonSelected(season.seasonNumber) },
                label = { Text(season.displayName) },
            )
        }
    }
    if (selectedSeason != null) {
        Spacer(modifier = Modifier.height(16.dp))
        SeasonHeader(selectedSeason, uiState.episodes)
    }
    when {
        uiState.isSeasonLoading && uiState.episodes.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }

        uiState.seasonErrorMessage != null && uiState.episodes.isEmpty() -> {
            Text(
                text = uiState.seasonErrorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(onClick = onRetry) { Text("Try again") }
        }

        else -> {
            uiState.episodes.forEach { episode ->
                EpisodeCard(
                    episode = episode,
                    trackingEnabled = uiState.entryId != null,
                    isUpdating = episode.id in uiState.updatingEpisodeIds,
                    onWatchedChange = { watched -> onEpisodeWatchedChange(episode, watched) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (uiState.seasonErrorMessage != null) {
                Text(
                    text = "Couldn't refresh this season. Showing saved episodes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun SeasonHeader(season: TvSeason, episodes: List<TvEpisode>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = season.name, style = MaterialTheme.typography.titleMedium)
            season.airDate?.let { airDate ->
                Text(
                    text = airDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "${episodes.count { it.isWatched }} / ${season.episodeCount}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun EpisodeCard(
    episode: TvEpisode,
    trackingEnabled: Boolean,
    isUpdating: Boolean,
    onWatchedChange: (Boolean) -> Unit,
) {
    val enabled = trackingEnabled && !isUpdating
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (episode.isWatched) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = episode.isWatched,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onWatchedChange,
            ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = TmdbImages.stillUrl(episode.stillPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(112.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metadata = listOfNotNull(
                    episode.airDate,
                    episode.runtimeMinutes?.let { "$it min" },
                ).joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Checkbox(
                checked = episode.isWatched,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    }
}

private val TvSeason.displayName: String
    get() = if (seasonNumber == 0) "Specials" else "Season $seasonNumber"

@Composable
private fun NextUnwatchedBanner(episode: TvEpisode) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next up",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "S${episode.seasonNumber}E${episode.episodeNumber} — ${episode.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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

@Composable
private fun RatingNotesDialog(
    onDismiss: () -> Unit,
    onConfirm: (rating: Int?, notes: String?) -> Unit,
) {
    var sliderValue by remember { mutableStateOf(5f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this title") },
        text = {
            Column {
                Text(
                    text = "Rating: ${sliderValue.toInt()} / 10",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..10f,
                    steps = 8,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(sliderValue.toInt(), notes.ifBlank { null })
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        },
    )
}

@Composable
private fun PersonalRatingRow(
    rating: Int?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (rating != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your rating",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (rating != null) {
                    Text(
                        text = "$rating / 10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Tap to rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit rating",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NotesRow(
    notes: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!notes.isNullOrBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "Tap to add notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditRatingDialog(
    currentRating: Int?,
    onDismiss: () -> Unit,
    onConfirm: (rating: Int?) -> Unit,
) {
    var sliderValue by remember { mutableStateOf((currentRating ?: 5).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this title") },
        text = {
            Column {
                Text(
                    text = "Rating: ${sliderValue.toInt()} / 10",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..10f,
                    steps = 8,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.toInt()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentRating != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun EditNotesDialog(
    currentNotes: String?,
    onDismiss: () -> Unit,
    onConfirm: (notes: String?) -> Unit,
) {
    var notesText by remember { mutableStateOf(currentNotes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notes") },
        text = {
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notesText.ifBlank { null }) }) {
                Text("Save")
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
            onMarkAsWatchedClick = {},
            onEditRating = {},
            onEditNotes = {},
            onRewatch = {},
            onSeasonSelected = {},
            onEpisodeWatchedChange = { _, _ -> },
            onRetrySeason = {},
        )
    }
}
