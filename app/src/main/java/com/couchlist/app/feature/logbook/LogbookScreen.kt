package com.couchlist.app.feature.logbook

import android.text.format.DateUtils
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.couchlist.app.core.domain.model.LogAction
import com.couchlist.app.core.domain.model.LogMediaEntry
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.ui.components.TmdbImages
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookRoute(
    viewModel: LogbookViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Logbook") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LogbookFilterRow(
                activeFilter = uiState.activeFilter,
                onFilterChanged = viewModel::onFilterChanged,
            )
            if (uiState.entries.isEmpty()) {
                EmptyLogbook()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.entries, key = { it.entry.id }) { entry ->
                        LogEntryCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogbookFilterRow(
    activeFilter: LogAction?,
    onFilterChanged: (LogAction?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = activeFilter == null,
                onClick = { onFilterChanged(null) },
                label = { Text("All") },
            )
        }
        item {
            FilterChip(
                selected = activeFilter == LogAction.MOVIE_WATCHED,
                onClick = { onFilterChanged(LogAction.MOVIE_WATCHED) },
                label = { Text("Movies") },
            )
        }
        item {
            FilterChip(
                selected = activeFilter == LogAction.EPISODE_WATCHED,
                onClick = { onFilterChanged(LogAction.EPISODE_WATCHED) },
                label = { Text("Episodes") },
            )
        }
        item {
            FilterChip(
                selected = activeFilter == LogAction.COMPLETED,
                onClick = { onFilterChanged(LogAction.COMPLETED) },
                label = { Text("Completed") },
            )
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogMediaEntry) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = TmdbImages.posterUrl(entry.posterPath, size = "w92"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(45.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.mediaTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.entry.action.toLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        entry.entry.date,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                    ).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.entry.personalRating?.let { rating ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${rating.toFloat() / 2} / 5",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                entry.entry.notes?.let { notes ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLogbook() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your logbook is empty",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mark titles as watched to see them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LogAction.toLabel(): String = when (this) {
    LogAction.MOVIE_WATCHED -> "Watched movie"
    LogAction.EPISODE_WATCHED -> "Watched episode"
    LogAction.EPISODE_UNWATCHED -> "Unwatched episode"
    LogAction.COMPLETED -> "Completed"
}
