package com.example.lingoFlix.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.ui.components.SecondaryButton
import com.example.lingoFlix.ui.components.SectionHeader
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSpacing
import java.util.Locale

/**
 * The subtitle half of the media detail screen.
 *
 * Separated from MediaDetailSections.kt because subtitle management is the part
 * of this screen that will keep growing (downloads, embedded track extraction),
 * and it should not drag the header and metadata code along with it.
 */

/**
 * The subtitle track list, as lazy items rather than a nested column.
 *
 * Declared as a [LazyListScope] extension so the tracks scroll with the rest of
 * the page instead of inside their own scroll container, which would produce a
 * scroll area inside a scroll area.
 */
fun LazyListScope.subtitleSection(
    uiState: MediaDetailUiState,
    actions: MediaDetailActions,
    onOpenDialog: (DetailDialog) -> Unit,
) {
    // With no tracks the callout under the primary action already states the
    // problem and offers the picker, so an empty section here would repeat the
    // same message twice on one screen.
    if (uiState.tracks.isEmpty()) return

    item { SectionHeader(title = stringResource(R.string.detail_subtitles)) }

    items(uiState.tracks.size, key = { uiState.tracks[it].id }) { index ->
        val track = uiState.tracks[index]
        SubtitleTrackRow(
            track = track,
            onMakePrimary = { actions.onMakePrimary(track.id) },
            onAdjustSync = { onOpenDialog(DetailDialog.SyncOffset(track.id)) },
            onDelete = { onOpenDialog(DetailDialog.DeleteTrack(track.id)) },
        )
    }

    item {
        SecondaryButton(
            text = stringResource(R.string.detail_attach_subtitle),
            onClick = actions.onPickSubtitle,
            icon = Icons.Default.Subtitles,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.sm),
        )
    }
}

/**
 * One track, with everything needed to judge whether it is the right one:
 * language, where it came from, how many sentences it yielded, and any sync
 * offset already applied.
 */
@Composable
private fun SubtitleTrackRow(
    track: SubtitleTrack,
    onMakePrimary: () -> Unit,
    onAdjustSync: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.xs),
        shape = RoundedCornerShape(LingoRadius.md),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = LingoSpacing.md,
                end = LingoSpacing.xs,
                top = LingoSpacing.sm,
                bottom = LingoSpacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
                ) {
                    Text(
                        text = stringResource(languageLabel(track.language)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (track.isPrimary) PrimaryTrackBadge()
                }
                Text(
                    text = stringResource(sourceLabel(track.source)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.detail_sentence_count, track.sentenceCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Only surfaced once it is non-zero: a "+0.0" line on every
                // track would be noise on the common, correctly synced case.
                if (track.offsetMs != 0L) {
                    Text(
                        text = stringResource(
                            R.string.detail_sync_offset_value,
                            formatOffset(track.offsetMs),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            TrackMenu(
                canMakePrimary = !track.isPrimary,
                onMakePrimary = onMakePrimary,
                onAdjustSync = onAdjustSync,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun PrimaryTrackBadge() {
    Surface(
        shape = RoundedCornerShape(LingoRadius.pill),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LingoSpacing.sm, vertical = BADGE_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BADGE_PADDING),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.detail_primary),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun TrackMenu(
    canMakePrimary: Boolean,
    onMakePrimary: () -> Unit,
    onAdjustSync: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(value = false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.detail_track_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Omitted rather than disabled for the current primary: a greyed
            // entry would suggest the action is temporarily unavailable.
            if (canMakePrimary) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.detail_make_primary)) },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onMakePrimary()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_sync_offset)) },
                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAdjustSync()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_delete_subtitle)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * Signed seconds with one decimal, e.g. `+0.3`.
 *
 * [Locale.ROOT] keeps the decimal separator a dot so the sign and the number do
 * not reorder inside a right-to-left paragraph.
 */
internal fun formatOffset(offsetMs: Long): String =
    String.format(Locale.ROOT, "%+.1f", offsetMs / 1000.0)

/**
 * Hebrew name for a track's language code.
 *
 * Accepts both ISO 639-1 and 639-2 forms because subtitle files in the wild use
 * either, plus the legacy `iw` code Java still emits for Hebrew.
 */
@StringRes
internal fun languageLabel(code: String): Int = when (code.lowercase()) {
    "he", "heb", "iw" -> R.string.lang_he
    "ar", "ara" -> R.string.lang_ar
    "ru", "rus" -> R.string.lang_ru
    "es", "spa" -> R.string.lang_es
    "fr", "fra" -> R.string.lang_fr
    "de", "deu" -> R.string.lang_de
    "ja", "jpn" -> R.string.lang_ja
    "zh", "zho" -> R.string.lang_zh
    else -> R.string.lang_en
}

@StringRes
internal fun sourceLabel(source: SubtitleSourceType): Int = when (source) {
    SubtitleSourceType.MANUAL -> R.string.detail_source_manual
    SubtitleSourceType.AUTO_MATCH -> R.string.detail_source_auto
    SubtitleSourceType.EMBEDDED -> R.string.detail_source_embedded
    SubtitleSourceType.DOWNLOADED -> R.string.detail_source_downloaded
    SubtitleSourceType.AI_GENERATED -> R.string.detail_source_ai
}

private val BADGE_PADDING = 2.dp
