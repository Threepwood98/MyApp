package com.couchlist.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import com.couchlist.app.core.ui.theme.dimensions

object MediaArtworkDefaults {
    const val PosterAspectRatio = 2f / 3f
    const val BackdropAspectRatio = 16f / 9f
}

@Composable
fun MediaArtwork(
    model: Any?,
    contentDescription: String?,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val accessibilityModifier = contentDescription?.let { description ->
        Modifier.semantics { this.contentDescription = description }
    } ?: Modifier
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .then(accessibilityModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (placeholder != null) {
            placeholder()
        } else {
            DefaultArtworkPlaceholder()
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay?.invoke(this)
    }
}

@Composable
fun MediaPoster(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    MediaArtwork(
        model = model,
        contentDescription = contentDescription,
        aspectRatio = MediaArtworkDefaults.PosterAspectRatio,
        modifier = modifier,
        shape = shape,
        placeholder = placeholder,
        overlay = overlay,
    )
}

@Composable
fun MediaBackdrop(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    MediaArtwork(
        model = model,
        contentDescription = contentDescription,
        aspectRatio = MediaArtworkDefaults.BackdropAspectRatio,
        modifier = modifier,
        shape = shape,
        placeholder = placeholder,
        overlay = overlay,
    )
}

@Composable
private fun DefaultArtworkPlaceholder() {
    Box(
        modifier = Modifier
            .size(MaterialTheme.dimensions.minimumTouchTarget)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
        )
    }
}
