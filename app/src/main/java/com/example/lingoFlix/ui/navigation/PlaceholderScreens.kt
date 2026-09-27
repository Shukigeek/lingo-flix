package com.example.lingoFlix.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.EmptyState
import com.example.lingoFlix.ui.components.LingoScaffold
import com.example.lingoFlix.ui.components.LingoTopBar
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import com.example.lingoFlix.ui.theme.LingoSpacing

/**
 * Stand-in for destinations that are wired into navigation but not yet built.
 *
 * Having a real composable behind every route means the graph is complete and
 * type-checked from day one, so later steps only swap the body of a
 * `composable<Route.X>` block instead of restructuring the NavHost.
 *
 * @param name human-readable screen name, shown in the app bar.
 * @param onBack supplied for pushed destinations; null on bottom-bar tabs.
 */
@Composable
fun PlaceholderScreen(
    name: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    detail: String? = null,
) {
    LingoScaffold(
        modifier = modifier,
        topBar = { LingoTopBar(title = name, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LingoSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyState(
                icon = Icons.Default.Construction,
                title = name,
                body = detail ?: stringResource(R.string.placeholder_body),
            )
        }
    }
}

@Preview(name = "Placeholder light", showBackground = true, heightDp = 480)
@Composable
private fun PlaceholderLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlaceholderScreen(name = "תרגול")
        }
    }
}

@Preview(name = "Placeholder dark", showBackground = true, heightDp = 480)
@Composable
private fun PlaceholderDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlaceholderScreen(name = "נגן", onBack = {})
        }
    }
}
