package com.example.lingoFlix.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.model.MediaKindType
import com.example.lingoFlix.domain.model.MediaSource
import com.example.lingoFlix.domain.model.WatchProgress
import com.example.lingoFlix.ui.theme.LingoFlixTheme

/**
 * Home previews driven by hand-built [HomeUiState] values.
 *
 * No Hilt, no database, no navigation: the whole point of splitting
 * [HomeRoute] from [HomeScreen] is that these render instantly.
 */

private fun previewItem(
    id: Long,
    title: String,
    durationMs: Long = 21 * 60_000L + 40_000L,
    positionMs: Long? = null,
    seriesName: String? = null,
    season: Int? = null,
    episode: Int? = null,
) = MediaItem(
    id = id,
    folderId = null,
    title = title,
    fileUri = "file:///preview/$id.mp4",
    kind = if (seriesName != null) MediaKindType.EPISODE else MediaKindType.MOVIE,
    source = MediaSource.LOCAL,
    seriesName = seriesName,
    season = season,
    episode = episode,
    durationMs = durationMs,
    watchProgress = positionMs?.let {
        WatchProgress(
            mediaItemId = id,
            positionMs = it,
            completed = false,
            updatedAt = 0L,
        )
    },
)

private val PopulatedState = HomeUiState(
    continueWatching = listOf(
        previewItem(
            id = 1,
            title = "The One With Phoebe's Husband",
            positionMs = 12 * 60_000L + 31_000L,
            seriesName = "Friends",
            season = 2,
            episode = 4,
        ),
    ),
    recentLibrary = listOf(
        previewItem(1, "The One With Phoebe's Husband", seriesName = "Friends", season = 2, episode = 4),
        previewItem(2, "Inception", durationMs = 148 * 60_000L),
        previewItem(3, "Arrival", durationMs = 116 * 60_000L, positionMs = 30 * 60_000L),
        previewItem(4, "Spirited Away", durationMs = 125 * 60_000L),
    ),
    savedSentenceCount = 148,
    dueCount = 12,
    streakDays = 7,
    isLoading = false,
)

@Preview(name = "Home populated light", showBackground = true, heightDp = 900)
@Composable
private fun HomePopulatedLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = PopulatedState,
                actions = HomeActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Home populated dark", showBackground = true, heightDp = 900)
@Composable
private fun HomePopulatedDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = PopulatedState,
                actions = HomeActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Home empty", showBackground = true, heightDp = 700)
@Composable
private fun HomeEmptyPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = HomeUiState(isLoading = false, streakDays = 0),
                actions = HomeActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Home loading", showBackground = true, heightDp = 400)
@Composable
private fun HomeLoadingPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = HomeUiState(),
                actions = HomeActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
