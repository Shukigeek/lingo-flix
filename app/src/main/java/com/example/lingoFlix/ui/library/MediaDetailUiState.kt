package com.example.lingoFlix.ui.library

import androidx.compose.runtime.Immutable
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.model.SubtitleTrack

/**
 * Everything the media detail screen renders.
 *
 * Tracks are held separately from `item.subtitleTracks` because attaching or
 * re-syncing a subtitle must update the list immediately, and the subtitle
 * repository is the authority for that; the media item flow only carries a
 * denormalised copy.
 */
@Immutable
data class MediaDetailUiState(
    val item: MediaItem? = null,
    val tracks: List<SubtitleTrack> = emptyList(),
    val isLoading: Boolean = true,
    val isAttaching: Boolean = false,
    val message: String? = null,
) {
    /** The item was deleted or the id is stale; the screen shows an error state. */
    val isMissing: Boolean get() = !isLoading && item == null

    /**
     * Subtitles are what turns a video into a lesson, so the primary action
     * changes wording and destination based on this.
     */
    val hasSubtitles: Boolean get() = tracks.isNotEmpty()

    val primaryTrack: SubtitleTrack? get() = tracks.firstOrNull { it.isPrimary }
}

/**
 * Callbacks the detail screen fires.
 *
 * Navigation lambdas are supplied by the NavHost, the rest by
 * [MediaDetailRoute]; the ViewModel never sees this type.
 */
@Immutable
data class MediaDetailActions(
    val onBack: () -> Unit = {},
    val onPlay: (mediaId: Long) -> Unit = {},
    val onLearn: (mediaId: Long) -> Unit = {},
    /** Opens the system picker for a subtitle file. */
    val onPickSubtitle: () -> Unit = {},
    val onMakePrimary: (trackId: Long) -> Unit = {},
    val onSetOffset: (trackId: Long, offsetMs: Long) -> Unit = { _, _ -> },
    val onDeleteTrack: (trackId: Long) -> Unit = {},
    val onRename: (title: String) -> Unit = {},
    val onDelete: () -> Unit = {},
    val onConsumeMessage: () -> Unit = {},
)
