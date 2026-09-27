package com.example.lingoFlix.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.ui.components.SectionHeader
import com.example.lingoFlix.ui.theme.LingoRadius
import com.example.lingoFlix.ui.theme.LingoSpacing

/**
 * Folder and media listing, in whichever view mode is active.
 *
 * Folders always come first: a folder is a navigation target, and burying one
 * between posters makes a nested library feel like a dead end.
 */
@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    actions: LibraryActions,
    onFolderMenu: (Long) -> Unit,
    onItemMenu: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.viewMode) {
        // Grid tiles carry no overflow button: artwork is the point, and long
        // press already covers bulk actions there.
        ViewMode.GRID -> LibraryGrid(uiState, actions, onFolderMenu, modifier)
        ViewMode.LIST -> LibraryList(uiState, actions, onFolderMenu, onItemMenu, modifier)
    }
}

@Composable
private fun LibraryGrid(
    uiState: LibraryUiState,
    actions: LibraryActions,
    onFolderMenu: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        // Adaptive rather than a fixed count so the same code fills a phone and
        // a tablet without a window-size branch.
        columns = GridCells.Adaptive(minSize = GRID_MIN_TILE),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = LingoSpacing.md,
            end = LingoSpacing.md,
            // Deep enough that the last row clears the FAB once scrolled.
            bottom = FAB_CLEARANCE,
        ),
        horizontalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(LingoSpacing.md),
    ) {
        if (uiState.folders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = stringResource(R.string.library_section_folders))
            }
            items(items = uiState.folders, key = { "folder-${it.id}" }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { actions.onOpenFolder(folder.id) },
                    onMenu = { onFolderMenu(folder.id) },
                )
            }
        }
        if (uiState.items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = stringResource(R.string.library_section_items))
            }
            items(items = uiState.items, key = { "item-${it.id}" }) { item ->
                MediaGridTile(
                    item = item,
                    selected = item.id in uiState.selection,
                    selectionMode = uiState.isSelectionMode,
                    onClick = {
                        if (uiState.isSelectionMode) {
                            actions.onToggleSelection(item.id)
                        } else {
                            actions.onOpenMedia(item.id)
                        }
                    },
                    onLongClick = { actions.onToggleSelection(item.id) },
                )
            }
        }
    }
}

@Composable
private fun LibraryList(
    uiState: LibraryUiState,
    actions: LibraryActions,
    onFolderMenu: (Long) -> Unit,
    onItemMenu: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
    ) {
        if (uiState.folders.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.library_section_folders)) }
            items(items = uiState.folders, key = { "folder-${it.id}" }) { folder ->
                FolderRow(
                    folder = folder,
                    onClick = { actions.onOpenFolder(folder.id) },
                    onMenu = { onFolderMenu(folder.id) },
                )
            }
        }
        if (uiState.items.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.library_section_items)) }
            items(items = uiState.items, key = { "item-${it.id}" }) { item ->
                MediaListRow(
                    item = item,
                    selected = item.id in uiState.selection,
                    selectionMode = uiState.isSelectionMode,
                    onClick = {
                        if (uiState.isSelectionMode) {
                            actions.onToggleSelection(item.id)
                        } else {
                            actions.onOpenMedia(item.id)
                        }
                    },
                    onLongClick = { actions.onToggleSelection(item.id) },
                    onMenu = { onItemMenu(item.id) },
                )
            }
        }
    }
}

/**
 * Trail of ancestors leading to the current folder.
 *
 * Horizontally scrollable rather than truncated: with auto-created
 * `Series/Season NN` folders the trail is short but the names can be long.
 */
@Composable
fun BreadcrumbRow(
    breadcrumbs: List<LibraryFolder>,
    onOpenRoot: () -> Unit,
    onOpenFolder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = LingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            TextButton(onClick = onOpenRoot) {
                Text(
                    text = stringResource(R.string.library_root),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        items(items = breadcrumbs, key = { it.id }) { folder ->
            // Auto-mirrored so the trail reads right-to-left in Hebrew.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            TextButton(onClick = { onOpenFolder(folder.id) }) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Fixed-height card so a row of folders keeps a predictable grid rhythm. */
@Composable
private fun FolderCard(
    folder: LibraryFolder,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(FOLDER_CARD_HEIGHT),
        shape = RoundedCornerShape(LingoRadius.md),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        FolderContent(folder = folder, onMenu = onMenu)
    }
}

@Composable
private fun FolderRow(
    folder: LibraryFolder,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        FolderContent(folder = folder, onMenu = onMenu)
    }
}

/** Shared body of [FolderCard] and [FolderRow]; only the container differs. */
@Composable
private fun FolderContent(folder: LibraryFolder, onMenu: () -> Unit) {
    Row(
        modifier = Modifier.padding(
            start = LingoSpacing.md,
            end = LingoSpacing.sm,
            top = LingoSpacing.sm,
            bottom = LingoSpacing.sm,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.library_folder_items, folder.itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onMenu) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.library_folder_options),
            )
        }
    }
}

private val GRID_MIN_TILE = 140.dp

/** Button height plus its margin plus a caption line, so nothing hides behind the FAB. */
private val FAB_CLEARANCE = 112.dp
private val FOLDER_CARD_HEIGHT = 84.dp
