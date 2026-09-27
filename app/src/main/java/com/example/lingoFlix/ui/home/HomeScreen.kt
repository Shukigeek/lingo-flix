package com.example.lingoFlix.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.ui.components.EmptyState
import com.example.lingoFlix.ui.components.LoadingState
import com.example.lingoFlix.ui.components.MediaPoster
import com.example.lingoFlix.ui.components.PrimaryButton
import com.example.lingoFlix.ui.components.SectionHeader
import com.example.lingoFlix.ui.components.StatCard
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSizes
import com.example.lingoFlix.ui.theme.LingoSpacing
import java.util.Locale

/**
 * Stateful entry point for the home destination.
 *
 * Kept as a thin wrapper around [HomeScreen] so the layout itself can be
 * previewed and tested without Hilt, a NavController or a database.
 */
@Composable
fun HomeRoute(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, actions = actions, modifier = modifier)
}

/**
 * Home dashboard: resume playback, review progress, jump into the library.
 *
 * Fully stateless. Every interaction is reported through [actions]; the screen
 * has no idea what a route is.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> LoadingState(modifier = modifier)

        // Greeting stays pinned to the top even with nothing to show, so the
        // screen still reads as "home" rather than as an error page.
        uiState.isEmpty -> Column(modifier = modifier.fillMaxSize()) {
            GreetingRow(streakDays = uiState.streakDays)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Default.MovieFilter,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    actionLabel = stringResource(R.string.home_add_media),
                    onAction = actions.onAddMedia,
                )
            }
        }

        else -> HomeContent(uiState = uiState, actions = actions, modifier = modifier)
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LingoSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
    ) {
        GreetingRow(streakDays = uiState.streakDays)

        uiState.resumeItem?.let { item ->
            SectionHeader(title = stringResource(R.string.home_continue_watching))
            ContinueWatchingCard(
                item = item,
                onPlay = { actions.onPlay(item.id) },
                modifier = Modifier.padding(horizontal = LingoSpacing.md),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(LingoSpacing.md),
        ) {
            StatCard(
                icon = Icons.Default.School,
                value = uiState.dueCount.toString(),
                label = stringResource(R.string.home_stat_due_label),
                onClick = actions.onOpenPractice,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.Star,
                value = uiState.savedSentenceCount.toString(),
                label = stringResource(R.string.home_stat_saved_label),
                onClick = actions.onOpenSentences,
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.recentLibrary.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.home_my_library),
                actionLabel = stringResource(R.string.common_see_all),
                onAction = actions.onOpenLibrary,
            )
            LibraryRow(items = uiState.recentLibrary, onOpenMedia = actions.onOpenMedia)
        }

        Spacer(Modifier.height(LingoSpacing.sm))

        PrimaryButton(
            text = stringResource(R.string.home_add_media),
            onClick = actions.onAddMedia,
            icon = Icons.Default.Add,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LingoSpacing.md),
        )
    }
}

/** Greeting plus the streak chip, which is the screen's only always-on stat. */
@Composable
private fun GreetingRow(streakDays: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (streakDays > 0) {
            StreakChip(streakDays = streakDays)
        }
    }
}

@Composable
private fun StreakChip(streakDays: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LingoRadius.pill),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.home_streak, streakDays),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Hero card for the most recent in-progress item.
 *
 * Shows elapsed/total time and a progress bar rather than a bare "resume"
 * button so the user can tell at a glance whether they are five minutes or
 * fifty into an episode.
 */
@Composable
private fun ContinueWatchingCard(
    item: MediaItem,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val positionMs = item.watchProgress?.positionMs ?: 0L
    val elapsed = remember(positionMs) { formatDuration(positionMs) }
    val total = remember(item.durationMs) { formatDuration(item.durationMs) }
    val timeLabel = stringResource(R.string.home_progress_time, elapsed, total)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LingoRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.padding(LingoSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(LingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaPoster(
                posterUrl = item.metadata?.posterUrl,
                thumbnailPath = item.thumbnailPath,
                title = item.displayTitle,
                progressFraction = 0f,
                modifier = Modifier.size(width = 88.dp, height = 132.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.home_play),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryRow(
    items: List<MediaItem>,
    onOpenMedia: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = LingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
    ) {
        items(items = items, key = { it.id }) { item ->
            LibraryTile(item = item, onClick = { onOpenMedia(item.id) })
        }
    }
}

@Composable
private fun LibraryTile(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(LingoSizes.posterWidth),
        verticalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
    ) {
        MediaPoster(
            posterUrl = item.metadata?.posterUrl,
            thumbnailPath = item.thumbnailPath,
            title = item.displayTitle,
            progressFraction = item.progressFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(LingoSizes.posterHeight),
            onClick = onClick,
        )
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Formats a millisecond position as `m:ss` or `h:mm:ss`.
 *
 * Uses [Locale.ROOT] deliberately: a timecode must stay ASCII and unseparated
 * regardless of the device locale, which in this app is Hebrew.
 */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
