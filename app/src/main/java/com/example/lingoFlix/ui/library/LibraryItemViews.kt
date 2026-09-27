package com.example.lingoFlix.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.ui.components.MediaPoster
import com.example.lingoFlix.ui.home.formatDuration
import com.example.lingoFlix.ui.theme.LingoAccents
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSpacing
import java.util.Locale

/**
 * The two shapes a single library item can take, plus their badges.
 *
 * Split out of LibraryContent.kt so the list plumbing and the item chrome can
 * be reviewed independently; both are consumed only from that file.
 */

/**
 * Poster tile with the two facts that decide whether an item is usable: whether
 * it has subtitles, and how good the film is supposed to be.
 *
 * Long press rather than an overflow button starts multi-select, because a
 * permanent menu affordance on every tile would compete with the artwork.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaGridTile(
    item: MediaItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_ASPECT_RATIO)
                .clip(RoundedCornerShape(LingoRadius.md))
                .then(
                    if (selected) {
                        Modifier.border(
                            width = SELECTION_BORDER,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(LingoRadius.md),
                        )
                    } else {
                        Modifier
                    },
                )
                .combinedClickable(onLongClick = onLongClick, onClick = onClick),
        ) {
            MediaPoster(
                posterUrl = item.metadata?.posterUrl,
                thumbnailPath = item.thumbnailPath,
                title = item.displayTitle,
                progressFraction = item.progressFraction,
                modifier = Modifier.fillMaxSize(),
            )
            item.metadata?.imdbRating?.let { rating ->
                RatingChip(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(LingoSpacing.xs),
                )
            }
            if (!item.hasSubtitles) {
                NoSubtitlesBadge(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(LingoSpacing.xs),
                )
            }
            if (selectionMode) {
                SelectionMark(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(LingoSpacing.xs),
                )
            }
        }
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Compact row for scanning many titles quickly, when artwork does not matter. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaListRow(
    item: MediaItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(
                start = LingoSpacing.md,
                end = LingoSpacing.sm,
                top = LingoSpacing.sm,
                bottom = LingoSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LingoSpacing.md),
    ) {
        Box {
            MediaPoster(
                posterUrl = item.metadata?.posterUrl,
                thumbnailPath = item.thumbnailPath,
                title = item.displayTitle,
                progressFraction = 0f,
                modifier = Modifier.size(width = THUMB_WIDTH, height = THUMB_HEIGHT),
            )
            if (selectionMode) {
                SelectionMark(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(LingoSpacing.xs),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
        ) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SubtitleStatusRow(item = item)
            if (item.progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LIST_PROGRESS_HEIGHT),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
        IconButton(onClick = onMenu) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.library_item_options),
            )
        }
    }
}

/** Subtitle availability and runtime, the two facts a list row can fit. */
@Composable
private fun SubtitleStatusRow(item: MediaItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
    ) {
        Icon(
            imageVector = if (item.hasSubtitles) {
                Icons.Default.Subtitles
            } else {
                Icons.Default.SubtitlesOff
            },
            contentDescription = null,
            tint = if (item.hasSubtitles) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(
                if (item.hasSubtitles) {
                    R.string.library_has_subtitles
                } else {
                    R.string.library_no_subtitles
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.durationMs > 0) {
            Text(
                text = formatDuration(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingChip(rating: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LingoRadius.pill),
        // Scrim-backed rather than theme-coloured: it sits on arbitrary artwork.
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LingoSpacing.sm, vertical = BADGE_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BADGE_PADDING),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = LingoAccents.Warning,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.detail_rating, formatRating(rating)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * Error-coloured on purpose.
 *
 * An un-subtitled file cannot be learned from, which makes it a defect in the
 * library rather than a neutral property worth a grey label.
 */
@Composable
private fun NoSubtitlesBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LingoRadius.sm),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(R.string.library_no_subtitles),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = LingoSpacing.sm, vertical = BADGE_PADDING),
        )
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.White.copy(alpha = 0.7f)
        },
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(LingoRadius.pill))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
    )
}

/**
 * One decimal, ASCII digits.
 *
 * [Locale.ROOT] keeps the separator a dot: an IMDb score is a brand-shaped
 * number, not a locale-formatted one.
 */
internal fun formatRating(rating: Double): String =
    String.format(Locale.ROOT, "%.1f", rating)

internal val THUMB_WIDTH = 96.dp
internal val THUMB_HEIGHT = 56.dp
private val SELECTION_BORDER = 3.dp
private val LIST_PROGRESS_HEIGHT = 4.dp
private val BADGE_PADDING = 2.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f
