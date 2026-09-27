package com.example.lingoFlix.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.LingoTopBar

/**
 * The library screen's chrome: its two app bars and its floating action button.
 *
 * Kept apart from LibraryScreen.kt so that file stays a readable description of
 * the screen's structure rather than a wall of icon buttons.
 */

/** Browsing bar: search toggle, view-mode toggle and the overflow menu. */
@Composable
internal fun LibraryTopBar(
    title: String,
    onBack: (() -> Unit)?,
    searchVisible: Boolean,
    uiState: LibraryUiState,
    actions: LibraryActions,
    onToggleSearch: () -> Unit,
    onNewFolder: () -> Unit,
) {
    LingoTopBar(title = title, onBack = onBack) {
        IconButton(onClick = onToggleSearch) {
            Icon(
                imageVector = if (searchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                contentDescription = stringResource(
                    if (searchVisible) R.string.library_close_search else R.string.common_search,
                ),
            )
        }
        IconButton(
            onClick = {
                actions.onSetViewMode(
                    if (uiState.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID,
                )
            },
        ) {
            // The icon shows the mode the tap switches to, not the current one.
            Icon(
                imageVector = if (uiState.viewMode == ViewMode.GRID) {
                    Icons.AutoMirrored.Filled.ViewList
                } else {
                    Icons.Default.GridView
                },
                contentDescription = stringResource(
                    if (uiState.viewMode == ViewMode.GRID) {
                        R.string.library_view_list
                    } else {
                        R.string.library_view_grid
                    },
                ),
            )
        }
        LibraryOverflowMenu(
            uiState = uiState,
            actions = actions,
            onNewFolder = onNewFolder,
        )
    }
}

/**
 * Replaces the app bar while a multi-select is in progress.
 *
 * A dedicated bar rather than extra icons on the normal one: it makes the mode
 * change unmistakable and gives the user an obvious way out via the close icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.library_selection_count, count),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.library_clear_selection),
                )
            }
        },
        actions = {
            IconButton(onClick = onMove) {
                Icon(
                    // Auto-mirrored: the arrow implies a direction of travel.
                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = stringResource(R.string.library_move_to),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/**
 * Single FAB that expands into the two ways media enters the library.
 *
 * One file and a whole directory are different enough operations to deserve
 * separate entries, but not important enough to occupy two permanent buttons.
 */
@Composable
internal fun AddMediaFab(actions: LibraryActions) {
    var expanded by remember { mutableStateOf(value = false) }
    Box {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.library_add),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_add_video)) },
                leadingIcon = { Icon(Icons.Default.VideoFile, contentDescription = null) },
                onClick = {
                    expanded = false
                    actions.onPickVideos()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_add_folder)) },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                onClick = {
                    expanded = false
                    actions.onPickFolder()
                },
            )
        }
    }
}
