package com.couchlist.app.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaType
import com.couchlist.app.core.domain.model.WatchStatus
import com.couchlist.app.core.ui.theme.CouchlistTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = WatchStatus.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Couchlist") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                        )
                    }
                },
            )
        },
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
                WatchStatus.WATCHLIST -> uiState.watchlist
                WatchStatus.WATCHING -> uiState.watching
                WatchStatus.WATCHED -> uiState.watched
            }
            StatusList(status = tabs[selectedTab], items = items)
        }
    }
}

@Composable
private fun StatusList(
    status: WatchStatus,
    items: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${status.displayName} is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items = items, key = { it.id }) { item ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(text = item.mediaType.name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusListPreview() {
    CouchlistTheme {
        val sample = MediaItem(
            id = 1L,
            mediaType = MediaType.MOVIE,
            tmdbId = 550L,
            title = "Fight Club",
            posterPath = null,
            status = WatchStatus.WATCHLIST,
            sortOrder = 0,
            addedAt = 1L,
        )
        StatusList(
            status = WatchStatus.WATCHLIST,
            items = listOf(sample),
        )
    }
}