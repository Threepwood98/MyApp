package com.couchlist.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.couchlist.app.R
import com.couchlist.app.core.ui.theme.dimensions
import com.couchlist.app.core.ui.theme.spacing

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.loading),
) {
    Box(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaterialTheme.dimensions.contentMaxWidth)
                .padding(MaterialTheme.spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.medium))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ContentState(
        title = title,
        message = message,
        modifier = modifier,
        icon = icon ?: {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimensions.iconLarge),
            )
        },
        action = action,
    )
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.something_went_wrong),
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ContentState(
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        icon = {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(MaterialTheme.dimensions.iconLarge),
            )
        },
        action = action,
    )
}

@Composable
private fun ContentState(
    title: String,
    message: String?,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaterialTheme.dimensions.contentMaxWidth)
                .padding(MaterialTheme.spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.large))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (message != null) {
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.large))
                action()
            }
        }
    }
}
