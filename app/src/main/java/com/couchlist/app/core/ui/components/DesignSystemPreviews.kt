package com.couchlist.app.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.couchlist.app.core.ui.sample.SampleData
import com.couchlist.app.core.ui.theme.CouchlistTheme
import com.couchlist.app.core.ui.theme.dimensions
import com.couchlist.app.core.ui.theme.spacing

@Preview(
    name = "Media components - light",
    group = "Design system",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Preview(
    name = "Media components - dark",
    group = "Design system",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MediaComponentsPreview() {
    CouchlistTheme(dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            ) {
                SectionHeader(
                    title = "Continue enjoying",
                    supportingText = "A reusable foundation for every media category.",
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    SampleData.enjoying.take(3).forEach { item ->
                        LibraryItemCard(
                            title = item.media.title,
                            subtitle = item.media.category.name,
                            artworkModel = item.media.artworkUri,
                            onClick = {},
                            modifier = Modifier.width(MaterialTheme.dimensions.posterLargeWidth),
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
                LibraryItemRow(
                    title = SampleData.longTitle.media.title,
                    supportingText = "Long titles, missing metadata, and larger text remain readable.",
                    artworkModel = SampleData.longTitle.media.artworkUri,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                ListCard(
                    title = SampleData.favoriteLists.first().list.name,
                    supportingText = "${SampleData.favoriteLists.first().itemCount} items",
                    artworkModel = SampleData.favoriteLists.first().coverArtworkUri,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediaProgressRing(
                        progress = 0.42,
                        centerContent = {
                            Text("42", style = MaterialTheme.typography.labelSmall)
                        },
                    )
                    MediaProgressRing(progress = null)
                    MediaPoster(
                        model = null,
                        contentDescription = "Missing artwork example",
                        modifier = Modifier.width(MaterialTheme.dimensions.posterSmallWidth),
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Content states - light",
    group = "Design system",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
)
@Preview(
    name = "Content states - dark",
    group = "Design system",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ContentStatesPreview() {
    CouchlistTheme(dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    LoadingState(
                        label = "Loading your library",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    EmptyState(
                        title = "Nothing here yet",
                        message = "Saved media will appear here.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    ErrorState(
                        message = SampleData.simulatedError,
                        modifier = Modifier.fillMaxSize(),
                        action = {
                            Button(onClick = {}) {
                                Text("Try again")
                            }
                        },
                    )
                }
            }
        }
    }
}
