package com.example.lingoFlix.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.LibraryFolder

/**
 * Which modal the library screen currently shows.
 *
 * Modelled as one sealed value instead of a boolean per dialog so two dialogs
 * can never be open at once and the screen has a single piece of modal state.
 */
@Immutable
sealed interface LibraryDialog {
    data object None : LibraryDialog
    data object CreateFolder : LibraryDialog
    data object MoveSelection : LibraryDialog
    data object DeleteSelection : LibraryDialog
    data class FolderOptions(val folderId: Long) : LibraryDialog
    data class RenameFolder(val folderId: Long) : LibraryDialog
    data class DeleteFolder(val folderId: Long) : LibraryDialog
    data class ItemOptions(val mediaId: Long) : LibraryDialog
    data class RenameItem(val mediaId: Long) : LibraryDialog
    data class DeleteItem(val mediaId: Long) : LibraryDialog
}

/**
 * Renders whichever modal [dialog] selects.
 *
 * Centralised here so [LibraryScreen] stays a layout description and does not
 * grow a chain of `if (showX)` blocks.
 */
@Composable
fun LibraryDialogHost(
    dialog: LibraryDialog,
    uiState: LibraryUiState,
    actions: LibraryActions,
    onDismiss: () -> Unit,
) {
    // The options sheets escalate into a second dialog, so they need to be able
    // to replace themselves rather than only close.
    var current by remember(dialog) { mutableStateOf(dialog) }

    when (val active = current) {
        LibraryDialog.None -> Unit

        LibraryDialog.CreateFolder -> TextInputDialog(
            title = stringResource(R.string.library_new_folder),
            label = stringResource(R.string.library_folder_name_hint),
            initial = "",
            onConfirm = {
                actions.onCreateFolder(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        LibraryDialog.MoveSelection -> MoveTargetDialog(
            targets = uiState.moveTargets(),
            onPick = {
                actions.onMoveSelectionTo(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        LibraryDialog.DeleteSelection -> ConfirmDialog(
            title = stringResource(R.string.library_delete_selection, uiState.selection.size),
            body = stringResource(R.string.library_delete_selection_body),
            onConfirm = {
                actions.onDeleteSelection()
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is LibraryDialog.FolderOptions -> OptionsDialog(
            title = uiState.folderName(active.folderId),
            options = listOf(
                DialogOption(stringResource(R.string.common_rename), Icons.Default.Edit) {
                    current = LibraryDialog.RenameFolder(active.folderId)
                },
                DialogOption(stringResource(R.string.library_delete_folder), Icons.Default.Delete) {
                    current = LibraryDialog.DeleteFolder(active.folderId)
                },
            ),
            onDismiss = onDismiss,
        )

        is LibraryDialog.RenameFolder -> TextInputDialog(
            title = stringResource(R.string.library_rename_folder),
            label = stringResource(R.string.library_folder_name_hint),
            initial = uiState.folderName(active.folderId),
            onConfirm = {
                actions.onRenameFolder(active.folderId, it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is LibraryDialog.DeleteFolder -> ConfirmDialog(
            title = stringResource(R.string.library_delete_folder),
            body = stringResource(R.string.library_delete_folder_body),
            onConfirm = {
                actions.onDeleteFolder(active.folderId)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is LibraryDialog.ItemOptions -> OptionsDialog(
            title = uiState.itemTitle(active.mediaId),
            options = listOf(
                DialogOption(stringResource(R.string.common_rename), Icons.Default.Edit) {
                    current = LibraryDialog.RenameItem(active.mediaId)
                },
                DialogOption(stringResource(R.string.library_delete_item), Icons.Default.Delete) {
                    current = LibraryDialog.DeleteItem(active.mediaId)
                },
            ),
            onDismiss = onDismiss,
        )

        is LibraryDialog.RenameItem -> TextInputDialog(
            title = stringResource(R.string.library_rename_item),
            label = stringResource(R.string.library_item_name_hint),
            initial = uiState.itemTitle(active.mediaId),
            onConfirm = {
                actions.onRenameItem(active.mediaId, it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is LibraryDialog.DeleteItem -> ConfirmDialog(
            title = stringResource(R.string.library_delete_item),
            body = stringResource(R.string.library_delete_item_body),
            onConfirm = {
                actions.onDeleteItem(active.mediaId)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

// â”€â”€ Building blocks â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private data class DialogOption(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun OptionsDialog(
    title: String,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    TextButton(
                        onClick = option.onClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(option.icon, contentDescription = null)
                        Text(
                            text = option.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/**
 * Destination picker for a multi-select move.
 *
 * Offers the library root, the ancestors the user came through and the folders
 * visible here. The repository only exposes one level of children at a time, so
 * an arbitrary folder elsewhere in the tree is not reachable from this dialog.
 */
@Composable
private fun MoveTargetDialog(
    targets: List<LibraryFolder>,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_move_to)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null)
                    Text(
                        text = stringResource(R.string.library_root),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                    )
                }
                targets.forEach { folder ->
                    TextButton(
                        onClick = { onPick(folder.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Text(
                            text = folder.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun LibraryUiState.folderName(id: Long): String =
    folders.firstOrNull { it.id == id }?.name.orEmpty()

private fun LibraryUiState.itemTitle(id: Long): String =
    items.firstOrNull { it.id == id }?.title.orEmpty()

/** Ancestors plus the folders visible at this level, without duplicates. */
private fun LibraryUiState.moveTargets(): List<LibraryFolder> =
    (breadcrumbs + folders).distinctBy { it.id }
