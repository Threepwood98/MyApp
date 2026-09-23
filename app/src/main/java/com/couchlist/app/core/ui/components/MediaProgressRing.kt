package com.couchlist.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import com.couchlist.app.R
import com.couchlist.app.core.ui.theme.dimensions
import com.couchlist.app.core.ui.theme.spacing
import java.text.NumberFormat

enum class MediaProgressRingSize {
    COMPACT,
    NORMAL,
}

@Composable
fun MediaProgressRing(
    progress: Double?,
    modifier: Modifier = Modifier,
    size: MediaProgressRingSize = MediaProgressRingSize.NORMAL,
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val normalizedProgress = progress?.coerceIn(0.0, 1.0)
    val target = normalizedProgress?.toFloat() ?: 0f
    val animatedProgress by animateFloatAsState(targetValue = target, label = "media progress")
    val diameter = when (size) {
        MediaProgressRingSize.COMPACT -> MaterialTheme.dimensions.progressCompact
        MediaProgressRingSize.NORMAL -> MaterialTheme.dimensions.progressNormal
    }
    val strokeWidth = when (size) {
        MediaProgressRingSize.COMPACT -> MaterialTheme.dimensions.progressCompactStroke
        MediaProgressRingSize.NORMAL -> MaterialTheme.dimensions.progressNormalStroke
    }
    val stateDescription = normalizedProgress?.let {
        stringResource(
            R.string.progress_percent,
            NumberFormat.getPercentInstance().format(it),
        )
    } ?: stringResource(R.string.progress_unknown)
    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics {
                this.stateDescription = stateDescription
                progressBarRangeInfo = normalizedProgress?.let {
                    ProgressBarRangeInfo(target, 0f..1f)
                } ?: ProgressBarRangeInfo.Indeterminate
            },
        contentAlignment = Alignment.Center,
    ) {
        if (progress == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = strokeWidth,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            CircularProgressIndicator(
                progress = { animatedProgress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = strokeWidth,
                modifier = Modifier.matchParentSize(),
            )
        }
        centerContent?.invoke(this)
    }
}

@Composable
fun MediaProgressBadge(
    progress: Double?,
    modifier: Modifier = Modifier,
    size: MediaProgressRingSize = MediaProgressRingSize.COMPACT,
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = modifier,
    ) {
        MediaProgressRing(
            progress = progress,
            size = size,
            centerContent = centerContent,
            modifier = Modifier.padding(MaterialTheme.spacing.extraSmall),
        )
    }
}
