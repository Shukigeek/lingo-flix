package com.example.lingoFlix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import com.example.lingoFlix.ui.theme.LingoSizes
import com.example.lingoFlix.ui.theme.LingoSpacing

/**
 * Previews for the shared component library.
 *
 * Kept in a separate file from LingoUi.kt so the production file stays small and
 * a reviewer can scan the API without scrolling past preview scaffolding.
 */

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LingoSpacing.md),
            content = { content() },
        )
    }
}

@Preview(name = "Buttons light", showBackground = true)
@Composable
private fun ButtonsLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        PreviewSurface {
            PrimaryButton(
                text = "התחל ללמוד",
                onClick = {},
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "הוסף סרט",
                onClick = {},
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(text = "מושבת", onClick = {}, enabled = false)
        }
    }
}

@Preview(name = "Buttons dark", showBackground = true)
@Composable
private fun ButtonsDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        PreviewSurface {
            PrimaryButton(
                text = "התחל ללמוד",
                onClick = {},
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(text = "הוסף סרט", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(name = "Stat cards light", showBackground = true)
@Composable
private fun StatCardsLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        PreviewSurface {
            Row(horizontalArrangement = Arrangement.spacedBy(LingoSpacing.md)) {
                StatCard(
                    icon = Icons.Default.School,
                    value = "12",
                    label = "משפטים לחזרה",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Default.Star,
                    value = "148",
                    label = "משפטים שמורים",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(name = "Stat cards dark", showBackground = true)
@Composable
private fun StatCardsDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        PreviewSurface {
            Row(horizontalArrangement = Arrangement.spacedBy(LingoSpacing.md)) {
                StatCard(
                    icon = Icons.Default.School,
                    value = "3",
                    label = "משפטים לחזרה",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Default.Star,
                    value = "0",
                    label = "משפטים שמורים",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(name = "Section header", showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SectionHeader(title = "הספרייה שלי", actionLabel = "הצג הכל", onAction = {})
        }
    }
}

@Preview(name = "Top bar", showBackground = true)
@Composable
private fun TopBarPreview() {
    LingoFlixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LingoTopBar(title = "הספרייה שלי", onBack = {})
        }
    }
}

@Preview(name = "Empty state light", showBackground = true, heightDp = 420)
@Composable
private fun EmptyStateLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                icon = Icons.Default.MovieFilter,
                title = "הספרייה ריקה",
                body = "הוסף סרט או פרק כדי להתחיל ללמוד",
                actionLabel = "הוסף סרט",
                onAction = {},
            )
        }
    }
}

@Preview(name = "Empty state dark", showBackground = true, heightDp = 420)
@Composable
private fun EmptyStateDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                icon = Icons.Default.MovieFilter,
                title = "הספרייה ריקה",
                body = "הוסף סרט או פרק כדי להתחיל ללמוד",
                actionLabel = "הוסף סרט",
                onAction = {},
            )
        }
    }
}

@Preview(name = "Loading", showBackground = true, heightDp = 220)
@Composable
private fun LoadingStatePreview() {
    LingoFlixTheme { LoadingState() }
}

@Preview(name = "Error", showBackground = true, heightDp = 320)
@Composable
private fun ErrorStatePreview() {
    LingoFlixTheme { ErrorState(message = "משהו השתבש", onRetry = {}) }
}

@Preview(name = "Poster placeholder light", showBackground = true)
@Composable
private fun MediaPosterLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(LingoSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
            ) {
                MediaPoster(
                    posterUrl = null,
                    thumbnailPath = null,
                    title = "Friends S02E04",
                    progressFraction = 0.58f,
                    modifier = Modifier.size(LingoSizes.posterWidth, LingoSizes.posterHeight),
                )
                MediaPoster(
                    posterUrl = null,
                    thumbnailPath = null,
                    title = "Inception",
                    progressFraction = 0f,
                    modifier = Modifier.size(LingoSizes.posterWidth, LingoSizes.posterHeight),
                )
            }
        }
    }
}

@Preview(name = "Poster placeholder dark", showBackground = true)
@Composable
private fun MediaPosterDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaPoster(
                posterUrl = null,
                thumbnailPath = null,
                title = "Friends S02E04",
                progressFraction = 0.2f,
                modifier = Modifier
                    .padding(LingoSpacing.md)
                    .width(LingoSizes.posterWidth)
                    .height(LingoSizes.posterHeight),
            )
        }
    }
}

@Preview(name = "Scaffold", showBackground = true, heightDp = 300)
@Composable
private fun LingoScaffoldPreview() {
    LingoFlixTheme {
        LingoScaffold(
            topBar = { LingoTopBar(title = "בית") },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                SectionHeader(title = "המשך מאיפה שהפסקת")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .padding(LingoSpacing.md)
                        .fillMaxWidth()
                        .height(96.dp),
                ) {}
            }
        }
    }
}
