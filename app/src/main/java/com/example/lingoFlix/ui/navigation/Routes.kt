package com.example.lingoFlix.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lingoFlix.R
import kotlinx.serialization.Serializable

/**
 * Every destination in the app.
 *
 * Routes are serializable data classes rather than strings, so arguments are
 * checked at compile time and there is no manual URL encoding to get wrong.
 */
sealed interface Route {

    /** The five destinations reachable from the bottom bar. */
    sealed interface TopLevel : Route

    @Serializable
    data object Home : TopLevel

    /** Library root; also the bottom-bar entry for the library tab. */
    @Serializable
    data object LibraryRoot : TopLevel

    /** A specific folder inside the library tree. */
    @Serializable
    data class Library(val folderId: Long) : Route

    @Serializable
    data object Sentences : TopLevel

    @Serializable
    data object Practice : TopLevel

    @Serializable
    data object Profile : TopLevel

    @Serializable
    data class MediaDetail(val mediaId: Long) : Route

    /** @param startMs `-1` resumes from the saved position. */
    @Serializable
    data class Player(val mediaId: Long, val startMs: Long = -1L) : Route

    @Serializable
    data class SentenceDetail(val sentenceId: Long) : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Discovery : Route
}

/**
 * Bottom navigation entries, in display order.
 */
enum class TopLevelDestination(
    val route: Route.TopLevel,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(
        route = Route.Home,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    LIBRARY(
        route = Route.LibraryRoot,
        labelRes = R.string.nav_library,
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
    ),
    SENTENCES(
        route = Route.Sentences,
        labelRes = R.string.nav_sentences,
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarOutline,
    ),
    PRACTICE(
        route = Route.Practice,
        labelRes = R.string.nav_practice,
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School,
    ),
    PROFILE(
        route = Route.Profile,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}
