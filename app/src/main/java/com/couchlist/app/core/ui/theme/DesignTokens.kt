package com.couchlist.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class CouchlistSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val extraExtraLarge: Dp = 32.dp,
    val section: Dp = 28.dp,
)

@Immutable
data class CouchlistDimensions(
    val minimumTouchTarget: Dp = 48.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 36.dp,
    val artworkRowWidth: Dp = 48.dp,
    val artworkRowHeight: Dp = 72.dp,
    val posterSmallWidth: Dp = 72.dp,
    val posterMediumWidth: Dp = 112.dp,
    val posterLargeWidth: Dp = 144.dp,
    val progressCompact: Dp = 32.dp,
    val progressNormal: Dp = 64.dp,
    val progressCompactStroke: Dp = 3.dp,
    val progressNormalStroke: Dp = 5.dp,
    val contentMaxWidth: Dp = 960.dp,
)

@Immutable
data class CouchlistElevations(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
)

internal val DefaultCouchlistSpacing = CouchlistSpacing()
internal val DefaultCouchlistDimensions = CouchlistDimensions()
internal val DefaultCouchlistElevations = CouchlistElevations()

internal val LocalCouchlistSpacing = staticCompositionLocalOf { DefaultCouchlistSpacing }
internal val LocalCouchlistDimensions = staticCompositionLocalOf { DefaultCouchlistDimensions }
internal val LocalCouchlistElevations = staticCompositionLocalOf { DefaultCouchlistElevations }

val MaterialTheme.spacing: CouchlistSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalCouchlistSpacing.current

val MaterialTheme.dimensions: CouchlistDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalCouchlistDimensions.current

val MaterialTheme.elevations: CouchlistElevations
    @Composable
    @ReadOnlyComposable
    get() = LocalCouchlistElevations.current
