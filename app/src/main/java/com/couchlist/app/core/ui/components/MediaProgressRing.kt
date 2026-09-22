package com.couchlist.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MediaProgressRingSize(
    internal val diameter: Dp,
    internal val strokeWidth: Dp,
) {
    COMPACT(32.dp, 3.dp),
    NORMAL(64.dp, 5.dp),
}

@Composable
fun MediaProgressRing(
    progress: Double,
    modifier: Modifier = Modifier,
    size: MediaProgressRingSize = MediaProgressRingSize.NORMAL,
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val target = progress.coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(targetValue = target, label = "media progress")
    Box(
        modifier = modifier.size(size.diameter),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = size.strokeWidth,
            modifier = Modifier.matchParentSize(),
        )
        centerContent?.invoke(this)
    }
}
