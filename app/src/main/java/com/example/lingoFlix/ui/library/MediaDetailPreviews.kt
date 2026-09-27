package com.example.lingoFlix.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.ui.theme.LingoFlixTheme

/**
 * Media detail previews driven by hand-built [MediaDetailUiState] values.
 *
 * `locale = "he"` on every preview so the renderer mirrors the layout: the
 * backdrop title, the metadata row and the track rows all anchor to the start
 * edge, and only an RTL render shows whether that is the right edge.
 */

private val SubtitledItem = previewMedia(
    id = 1,
    title = "Arrival",
    durationMs = 116 * 60_000L,
    rating = 7.9,
    year = 2016,
    genres = listOf("מדע בדיוני", "דרמה"),
    overview = "בלשנית מגויסת על ידי הצבא כדי לתקשר עם חייזרים שנחתו על כדור " +
        "הארץ, ותוך כדי כך מגלה שהשפה שהם מדברים משנה את הדרך שבה היא תופסת את " +
        "הזמן עצמו. ככל שהיא מתקרבת לפענוח, ההבחנה בין זיכרון לנבואה מיטשטשת.",
)

private val PopulatedState = MediaDetailUiState(
    item = SubtitledItem,
    tracks = listOf(
        previewTrack(1, 1, "en", isPrimary = true, sentenceCount = 1_184),
        previewTrack(
            id = 2,
            mediaItemId = 1,
            language = "he",
            sentenceCount = 1_170,
            offsetMs = 300L,
            source = SubtitleSourceType.AUTO_MATCH,
        ),
    ),
    isLoading = false,
)

@Preview(name = "Detail with subtitles", showBackground = true, heightDp = 1000, locale = "he")
@Composable
private fun MediaDetailPopulatedPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaDetailScreen(
                uiState = PopulatedState,
                actions = MediaDetailActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Detail without subtitles", showBackground = true, heightDp = 1000, locale = "he")
@Composable
private fun MediaDetailNoSubtitlesPreview() {
    LingoFlixTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaDetailScreen(
                uiState = MediaDetailUiState(
                    item = previewMedia(
                        id = 2,
                        title = "The One With Phoebe's Husband",
                        durationMs = 22 * 60_000L,
                        seriesName = "Friends",
                        season = 2,
                        episode = 4,
                    ),
                    isLoading = false,
                ),
                actions = MediaDetailActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Detail loading", showBackground = true, heightDp = 400, locale = "he")
@Composable
private fun MediaDetailLoadingPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaDetailScreen(
                uiState = MediaDetailUiState(),
                actions = MediaDetailActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Detail missing", showBackground = true, heightDp = 500, locale = "he")
@Composable
private fun MediaDetailMissingPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaDetailScreen(
                uiState = MediaDetailUiState(isLoading = false),
                actions = MediaDetailActions(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
