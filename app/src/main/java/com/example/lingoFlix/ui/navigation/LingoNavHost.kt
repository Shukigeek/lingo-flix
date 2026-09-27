package com.example.lingoFlix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.DiscoveryScreen
import com.example.lingoFlix.ui.SettingsScreen
import com.example.lingoFlix.ui.home.HomeActions
import com.example.lingoFlix.ui.home.HomeRoute

/**
 * The app's single navigation graph.
 *
 * Every destination is declared with a type-safe `composable<Route.X>`, so route
 * arguments are checked by the compiler and there is no string building or URL
 * encoding anywhere in the app.
 *
 * Screens receive plain lambdas rather than the [NavHostController] itself: that
 * keeps them previewable and stops navigation logic leaking into ViewModels.
 */
@Composable
fun LingoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier,
    ) {
        composable<Route.Home> {
            val actions = remember(navController) {
                HomeActions(
                    onPlay = { mediaId -> navController.navigate(Route.Player(mediaId)) },
                    onOpenLibrary = { navController.navigate(Route.LibraryRoot) },
                    onOpenSentences = { navController.navigate(Route.Sentences) },
                    onOpenPractice = { navController.navigate(Route.Practice) },
                    // TODO(step 3): replace with the library import picker once
                    //  LibraryScreen owns the SAF launchers.
                    onAddMedia = { navController.navigate(Route.LibraryRoot) },
                    onOpenMedia = { mediaId -> navController.navigate(Route.MediaDetail(mediaId)) },
                )
            }
            HomeRoute(actions = actions)
        }

        // TODO(step 3): replace with LibraryScreen (folder tree, import, search).
        composable<Route.LibraryRoot> {
            PlaceholderScreen(name = stringResource(R.string.library_title))
        }

        // TODO(step 3): replace with LibraryScreen scoped to Route.Library.folderId.
        composable<Route.Library> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Library>()
            PlaceholderScreen(
                name = stringResource(R.string.library_title),
                onBack = navController::popBackStack,
                detail = "folderId = ${route.folderId}",
            )
        }

        // TODO(step 4): replace with MediaDetailScreen (subtitle tracks, sync offset).
        composable<Route.MediaDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.MediaDetail>()
            PlaceholderScreen(
                name = stringResource(R.string.detail_watch),
                onBack = navController::popBackStack,
                detail = "mediaId = ${route.mediaId}",
            )
        }

        // TODO(step 5): replace with PlayerScreen. Declared full screen: MainActivity
        //  hides the bottom bar and drops content insets for this route.
        composable<Route.Player> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Player>()
            PlaceholderScreen(
                name = stringResource(R.string.player_translation),
                onBack = navController::popBackStack,
                detail = "mediaId = ${route.mediaId}, startMs = ${route.startMs}",
            )
        }

        // TODO(step 6): replace with SentencesScreen (saved list, filters, search).
        composable<Route.Sentences> {
            PlaceholderScreen(name = stringResource(R.string.sentences_title))
        }

        // TODO(step 6): replace with SentenceDetailScreen (clip replay, notes).
        composable<Route.SentenceDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.SentenceDetail>()
            PlaceholderScreen(
                name = stringResource(R.string.sentences_title),
                onBack = navController::popBackStack,
                detail = "sentenceId = ${route.sentenceId}",
            )
        }

        // TODO(step 7): replace with PracticeScreen (SM-2 review session).
        composable<Route.Practice> {
            PlaceholderScreen(name = stringResource(R.string.practice_title))
        }

        // TODO(step 8): replace with a rebuilt ProfileScreen. The legacy
        //  ui/ProfileScreen.kt needs userName/totalXP/currentStreak, which only
        //  exist in the removed MainActivity state, so it is not reused here.
        composable<Route.Profile> {
            PlaceholderScreen(name = stringResource(R.string.profile_title))
        }

        // TODO(step 3): replace with a search screen backed by
        //  LibraryRepository.searchItems and SentenceRepository.search.
        composable<Route.Search> {
            PlaceholderScreen(
                name = stringResource(R.string.common_search),
                onBack = navController::popBackStack,
            )
        }

        // Reuses the legacy settings screen unchanged: it is self-contained and
        // no longer reads the API key it still accepts.
        composable<Route.Settings> {
            SettingsScreen(
                currentApiKey = "",
                onSaveApiKey = {},
                onBack = navController::popBackStack,
            )
        }

        // Reuses the legacy discovery screen; it only needs a user id.
        composable<Route.Discovery> {
            DiscoveryScreen(userId = DEFAULT_USER_ID)
        }
    }
}

/**
 * Routes that take over the whole window.
 *
 * Used by MainActivity to hide the bottom bar and enter immersive mode; kept
 * next to the graph so adding a full-screen route is a single edit.
 */
internal fun NavDestination?.isFullScreenRoute(): Boolean = isOn(Route.Player::class)

/** Placeholder identity until accounts are part of the rebuilt domain layer. */
private const val DEFAULT_USER_ID = "guest"
