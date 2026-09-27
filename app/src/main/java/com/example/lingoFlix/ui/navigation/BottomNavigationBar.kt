package com.example.lingoFlix.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import kotlin.reflect.KClass

/**
 * Bottom navigation for the five top-level destinations.
 *
 * Driven entirely by [TopLevelDestination], so adding a tab is a one-line change
 * in Routes.kt and nothing here needs touching.
 */
@Composable
fun LingoBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = NavigationBarDefaults.Elevation,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination.isOn(destination.route::class)
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) {
                            destination.selectedIcon
                        } else {
                            destination.unselectedIcon
                        },
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Switches tabs the way Material expects.
 *
 * `popUpTo(startDestination) { saveState = true }` plus `restoreState` keeps one
 * back stack per tab, and `launchSingleTop` stops repeated taps from piling up
 * duplicate copies of the same screen.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * True when [routeClass] is anywhere in the destination's hierarchy.
 *
 * Checking the hierarchy rather than the leaf keeps the correct tab highlighted
 * when a nested destination is pushed on top of it.
 */
internal fun NavDestination?.isOn(routeClass: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(routeClass) } == true

@Preview(name = "Bottom bar light")
@Composable
private fun LingoBottomBarLightPreview() {
    LingoFlixTheme(darkTheme = false) {
        LingoBottomBar(navController = rememberNavController())
    }
}

@Preview(name = "Bottom bar dark")
@Composable
private fun LingoBottomBarDarkPreview() {
    LingoFlixTheme(darkTheme = true) {
        LingoBottomBar(navController = rememberNavController())
    }
}
