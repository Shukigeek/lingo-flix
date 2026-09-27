package com.example.lingoFlix.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lingoFlix.domain.model.FolderKindType
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.model.MediaKindType
import com.example.lingoFlix.domain.model.MediaMetadata
import com.example.lingoFlix.domain.model.MediaSource
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.domain.model.WatchProgress
import com.example.lingoFlix.ui.theme.LingoFlixTheme

/**
 * Library previews driven by hand-built [LibraryUiState] values.
 *
 * Every preview declares `locale = "he"` so the renderer lays the screen out
 * right to left, which is the only configuration the app ever ships in; a
 * left-to-right preview would hide exactly the bugs these are here to catch.
 */

internal fun previewTrack(
    id: Long,
    mediaItemId: Long,
    language: String,
    isPrimary: Boolean = false,
    sentenceCount: Int = 820,
    offsetMs: Long = 0L,
    source: SubtitleSourceType = SubtitleSourceType.MANUAL,
) = SubtitleTrack(
    id = id,
    mediaItemId = mediaItemId,
    language = language,
    filePath = "/data/subtitles/$id.srt",
    source = source,
    offsetMs = offsetMs,
    isPrimary = isPrimary,
    sentenceCount = sentenceCount,
)

internal fun previewMedia(
    id: Long,
    title: String,
    durationMs: Long = 113 * 60_000L,
    positionMs: Long? = null,
    rating: Double? = null,
    year: Int? = null,
    genres: List<String> = emptyList(),
    overview: String? = null,
    tracks: List<SubtitleTrack> = emptyList(),
    seriesName: String? = null,
    season: Int? = null,
    episode: Int? = null,
) = MediaItem(
    id = id,
    folderId = null,
    title = title,
    fileUri = "content://preview/$id",
    kind = if (seriesName != null) MediaKindType.EPISODE else MediaKindType.MOVIE,
    source = MediaSource.LOCAL,
    seriesName = seriesName,
    season = season,
    episode = episode,
    durationMs = durationMs,
    metadata = MediaMetadata(
        year = year,
        imdbRating = rating,
        genres = genres,
        overview = overview,
    ),
    subtitleTracks = tracks,
    watchProgress = positionMs?.let {
        WatchProgress(mediaItemId = id, positionMs = it, completed = false, updatedAt = 0L)
    },
)

private val PreviewFolders = listOf(
    LibraryFolder(id = 10, parentId = null, name = "Friends", kind = FolderKindType.SERIES, itemCount = 24),
    LibraryFolder(id = 11, parentId = null, name = "סרטי ילדים", kind = FolderKindType.CUSTOM, itemCount = 6),
)

private val PreviewItems = listOf(
    previewMedia(
        id = 1,
        title = "Inception",
        durationMs = 148 * 60_000L,
        rating = 8.8,
        year = 2010,
        tracks = listOf(previewTrack(1, 1, "en", isPrimary = true)),
    ),
    previewMedia(id = 2, title = "Arrival", durationMs = 116 * 60_000L, positionMs = 30 * 60_000L, rating = 7.9),
    previewMedia(
        id = 3,
        title = "Spirited Away",
        durationMs = 125 * 60_000L,
        rating = 8.6,
        tracks = listOf(previewTrack(3, 3, "he", isPrimary = true, sentenceCount = 640)),
    ),
    previewMedia(id = 4, title = "The Grand Budapest Hotel", durationMs = 99 * 60_000L),
)

private val PopulatedState = LibraryUiState(
    folders = PreviewFolders,
    items = PreviewItems,
    isLoading = false,
)

@Preview(name = "Library grid", showBackground = true, heightDp = 900, locale = "he")
@Composable
private fun LibraryGridPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LibraryScreen(
                uiState = PopulatedState,
                actions = LibraryActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Library list nested", showBackground = true, heightDp = 900, locale = "he")
@Composable
private fun LibraryListPreview() {
    LingoFlixTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LibraryScreen(
                uiState = PopulatedState.copy(
                    folderId = 10,
                    folderName = "Friends",
                    breadcrumbs = listOf(PreviewFolders.first()),
                    folders = emptyList(),
                    viewMode = ViewMode.LIST,
                ),
                actions = LibraryActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Library selection", showBackground = true, heightDp = 900, locale = "he")
@Composable
private fun LibrarySelectionPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LibraryScreen(
                uiState = PopulatedState.copy(selection = setOf(1L, 3L)),
                actions = LibraryActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Library empty", showBackground = true, heightDp = 700, locale = "he")
@Composable
private fun LibraryEmptyPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LibraryScreen(
                uiState = LibraryUiState(isLoading = false),
                actions = LibraryActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Library no results", showBackground = true, heightDp = 700, locale = "he")
@Composable
private fun LibraryNoResultsPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LibraryScreen(
                uiState = LibraryUiState(
                    isLoading = false,
                    filter = LibraryFilter(query = "zzz"),
                ),
                actions = LibraryActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Library loading", showBackground = true, heightDp = 400, locale = "he")
@Composable
private fun LibraryLoadingPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LibraryScreen(
                uiState = LibraryUiState(),
                actions = LibraryActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
