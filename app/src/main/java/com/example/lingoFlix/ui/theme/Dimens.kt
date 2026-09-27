package com.example.lingoFlix.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The app's spacing scale.
 *
 * Hard-coded `dp` values scattered across screens are the fastest way to end up
 * with a layout that looks almost-but-not-quite aligned. Every padding, gap and
 * inset in the UI layer should come from here.
 */
object LingoSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/**
 * Corner radii.
 *
 * [pill] is intentionally large rather than using `CircleShape` so that a pill
 * button keeps its shape when its height grows with large font settings.
 */
object LingoRadius {
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val pill = 999.dp
}

/** Fixed component sizes shared across screens. */
object LingoSizes {
    /** Primary/secondary action button height. */
    val buttonHeight = 56.dp

    /** Poster tile width in horizontal library rows. */
    val posterWidth = 132.dp

    /** Poster tile height, a 2:3 poster ratio against [posterWidth]. */
    val posterHeight = 198.dp

    /** Height of the thin progress bar drawn across the bottom of a poster. */
    val posterProgressHeight = 4.dp
}
