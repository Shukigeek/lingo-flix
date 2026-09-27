package com.example.lingoFlix.ui.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.ui.components.SecondaryButton
import com.example.lingoFlix.ui.components.SectionHeader
import com.example.lingoFlix.ui.theme.LingoAccents
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSpacing
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Backdrop with the title burned into a gradient scrim.
 *
 * The scrim is not decoration: poster art has arbitrary brightness, and white
 * title text over an unknown frame is unreadable roughly half the time.
 */
@Composable
fun DetailHeader(
    item: MediaItem,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artwork = item.metadata?.backdropUrl?.takeIf { it.isNotBlank() }
        ?: item.metadata?.posterUrl?.takeIf { it.isNotBlank() }
        ?: item.thumbnailPath?.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (artwork != null) {
            SubcomposeAsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = LingoSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    // Auto-mirrored so the arrow points the right way in Hebrew.
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.White,
                )
            }
            DetailOverflowMenu(onRename = onRename, onDelete = onDelete)
        }
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(LingoSpacing.md),
        )
    }
}

@Composable
private fun DetailOverflowMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.detail_more_actions),
                tint = Color.White,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_delete_item)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/** Year, runtime, genres and the IMDb score, in one scannable line. */
@Composable
fun MetadataRow(item: MediaItem, modifier: Modifier = Modifier) {
    val facts = buildList {
        item.metadata?.year?.let { add(it.toString()) }
        if (item.durationMs > 0) {
            add(
                stringResource(
                    R.string.detail_runtime,
                    TimeUnit.MILLISECONDS.toMinutes(item.durationMs).toInt(),
                ),
            )
        }
        item.metadata?.genres?.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(" Â· ")) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
    ) {
        Text(
            text = facts.joinToString(" Â· "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        item.metadata?.imdbRating?.let { rating -> ImdbChip(rating = rating) }
    }
}

/**
 * Read-only score badge.
 *
 * A [Surface] rather than a disabled chip: the rating is information, and a
 * greyed-out button reads as a broken control to both sighted users and
 * accessibility services.
 */
@Composable
private fun ImdbChip(rating: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LingoRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LingoSpacing.sm, vertical = LingoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = LingoAccents.Warning,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.detail_rating, formatRating(rating)),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Synopsis, collapsed to a few lines until the user asks for more.
 *
 * TMDB overviews run to a full paragraph, which would push the subtitle list â€”
 * the actionable part of this screen â€” below the fold on a phone.
 */
@Composable
fun OverviewSection(overview: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LingoSpacing.md)
            .animateContentSize(),
    ) {
        Text(
            text = stringResource(R.string.detail_overview),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = overview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_OVERVIEW_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (overview.length > OVERVIEW_EXPAND_THRESHOLD) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    stringResource(
                        if (expanded) R.string.detail_show_less else R.string.detail_show_more,
                    ),
                )
            }
        }
    }
}

/** Pushes the user towards attaching a subtitle when none exists yet. */
@Composable
fun AttachSubtitleCallout(onAttach: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LingoRadius.md),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(LingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.detail_no_subtitles_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.detail_no_subtitles_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            SecondaryButton(
                text = stringResource(R.string.detail_attach_subtitle),
                onClick = onAttach,
                icon = Icons.Default.Subtitles,
            )
        }
    }
}
private val HEADER_HEIGHT = 260.dp
private const val COLLAPSED_OVERVIEW_LINES = 4
private const val OVERVIEW_EXPAND_THRESHOLD = 180
