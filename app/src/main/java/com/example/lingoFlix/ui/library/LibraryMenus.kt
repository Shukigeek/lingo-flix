package com.example.lingoFlix.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.example.lingoFlix.R

/**
 * The library's overflow menu and its inline search field.
 *
 * Split from LibraryDialogs.kt so the modal flows and the always-available
 * controls can be read independently.
 */

/**
 * Sort order, filters and folder maintenance.
 *
 * All three live behind one overflow because none of them is used often enough
 * to deserve a permanent slot next to search and the view toggle. The filter
 * entries keep the menu open on purpose: toggling two filters in a row is the
 * normal case, and re-opening the menu each time would be hostile.
 */
@Composable
internal fun LibraryOverflowMenu(
    uiState: LibraryUiState,
    actions: LibraryActions,
    onNewFolder: () -> Unit,
) {
    var expanded by remember { mutableStateOf(value = false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.library_more_actions),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        // Disabled entry used as a section caption; Material 3 menus have no
        // header slot and a divider alone would not say what follows.
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_sort)) },
            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
            enabled = false,
            onClick = {},
        )
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(stringResource(order.labelRes())) },
                leadingIcon = {
                    RadioButton(selected = uiState.sortOrder == order, onClick = null)
                },
                onClick = {
                    expanded = false
                    actions.onSetSortOrder(order)
                },
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_filter_show_without_subtitles)) },
            leadingIcon = {
                Checkbox(checked = uiState.filter.showWithoutSubtitles, onCheckedChange = null)
            },
            onClick = {
                actions.onSetFilter(
                    uiState.filter.copy(
                        showWithoutSubtitles = !uiState.filter.showWithoutSubtitles,
                    ),
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_filter_only_unwatched)) },
            leadingIcon = {
                Checkbox(checked = uiState.filter.onlyUnwatched, onCheckedChange = null)
            },
            onClick = {
                actions.onSetFilter(
                    uiState.filter.copy(onlyUnwatched = !uiState.filter.onlyUnwatched),
                )
            },
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_new_folder)) },
            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
            onClick = {
                expanded = false
                onNewFolder()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_prune)) },
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            onClick = {
                expanded = false
                actions.onPruneMissing()
            },
        )
    }
}

/** Inline search box, shown only while the search action is toggled on. */
@Composable
internal fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(stringResource(R.string.library_search_hint)) },
        trailingIcon = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.library_clear_query))
            }
        },
    )
}

@StringRes
private fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.TITLE -> R.string.library_sort_title
    SortOrder.RECENTLY_ADDED -> R.string.library_sort_recently_added
    SortOrder.RECENTLY_WATCHED -> R.string.library_sort_recently_watched
}
