package com.example.lingoFlix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSizes
import com.example.lingoFlix.ui.theme.LingoSpacing
import com.example.lingoFlix.ui.theme.LingoTextStyles

/**
 * Content-bearing components: metric tiles and library artwork.
 *
 * Split out of LingoUi.kt purely to keep both files readable; the API contract
 * is identical and callers import from the same package.
 */

/**
 * Compact metric tile, e.g. "12 sentences due".
 *
 * The value uses a dedicated oversized style so a row of stat cards scans as
 * numbers first, labels second.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LingoRadius.lg),
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(LingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(text = value, style = LingoTextStyles.StatValue)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Artwork tile for a library item.
 *
 * Falls back from remote poster, to the locally extracted thumbnail, to a
 * tinted icon placeholder: most user-imported files have no TMDB match at all,
 * so a broken image would be the common case rather than the exception.
 *
 * @param progressFraction 0f..1f; a thin bar is drawn along the bottom edge when
 * greater than zero, which is how "already started" reads at a glance.
 */
@Composable
fun MediaPoster(
    posterUrl: String?,
    thumbnailPath: String?,
    title: String,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val artwork = posterUrl?.takeIf { it.isNotBlank() } ?: thumbnailPath?.takeIf { it.isNotBlank() }
    val description = stringResource(R.string.home_poster_of, title)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(LingoRadius.md))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (artwork == null) {
            PosterPlaceholder(description)
        } else {
            SubcomposeAsyncImage(
                model = artwork,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { PosterPlaceholder(description) },
                error = { PosterPlaceholder(description) },
            )
        }

        if (progressFraction > 0f) {
            // Decorative: the fraction is already announced by the item's label,
            // so the bar is removed from the accessibility tree.
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(LingoSizes.posterProgressHeight)
                    .clearAndSetSemantics { },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun PosterPlaceholder(description: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp),
        )
    }
}
