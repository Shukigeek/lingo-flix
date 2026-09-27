package com.example.lingoFlix.ui.home

import androidx.compose.runtime.Immutable
import com.example.lingoFlix.domain.model.MediaItem

/**
 * Everything the home screen renders, in one immutable snapshot.
 *
 * A single state object rather than several flows keeps the screen from showing
 * a half-updated mixture of old and new data, and makes previews trivial: build
 * a value, pass it in, done.
 */
@Immutable
data class HomeUiState(
    val continueWatching: List<MediaItem> = emptyList(),
    val recentLibrary: List<MediaItem> = emptyList(),
    val savedSentenceCount: Int = 0,
    val dueCount: Int = 0,
    val streakDays: Int = 0,
    val isLoading: Boolean = true,
) {
    /**
     * The single item promoted to the large hero card.
     *
     * Only the most recent in-progress item gets the hero treatment; the rest
     * stay in the library row so the screen does not turn into a wall of cards.
     */
    val resumeItem: MediaItem? get() = continueWatching.firstOrNull()

    /**
     * True once loading finished and there is genuinely nothing to show, which
     * is the only case where the empty state should replace the whole screen.
     */
    val isEmpty: Boolean
        get() = !isLoading && continueWatching.isEmpty() && recentLibrary.isEmpty()
}

/**
 * Navigation callbacks the home screen fires.
 *
 * Bundled into one stable class so adding a destination later does not change
 * every call site's parameter list, and so previews can pass a single no-op
 * instance. The ViewModel never sees this type: navigation stays in the NavHost.
 */
@Immutable
data class HomeActions(
    val onPlay: (mediaId: Long) -> Unit = {},
    val onOpenLibrary: () -> Unit = {},
    val onOpenSentences: () -> Unit = {},
    val onOpenPractice: () -> Unit = {},
    val onAddMedia: () -> Unit = {},
    val onOpenMedia: (mediaId: Long) -> Unit = {},
)
