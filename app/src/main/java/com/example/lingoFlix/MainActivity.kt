package com.example.lingoFlix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lingoFlix.ui.components.ImmersiveModeEffect
import com.example.lingoFlix.ui.components.LingoScaffold
import com.example.lingoFlix.ui.navigation.LingoBottomBar
import com.example.lingoFlix.ui.navigation.LingoNavHost
import com.example.lingoFlix.ui.navigation.isFullScreenRoute
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host.
 *
 * Deliberately almost empty: screen state now lives in ViewModels and
 * navigation state in the NavHost, so the activity only sets up edge-to-edge
 * drawing, the theme and the app shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LingoFlixTheme {
                AppShell()
            }
        }
    }
}

/**
 * App shell: bottom bar plus navigation graph.
 *
 * The player is the one destination that takes over the window, so the bottom
 * bar, the content insets and immersive mode are all derived from a single
 * route check instead of being toggled imperatively from callbacks.
 */
@Composable
private fun AppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val fullScreen = backStackEntry?.destination.isFullScreenRoute()

    ImmersiveModeEffect(enabled = fullScreen)

    LingoScaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { if (!fullScreen) LingoBottomBar(navController) },
        contentWindowInsets = if (fullScreen) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
    ) { innerPadding ->
        LingoNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
