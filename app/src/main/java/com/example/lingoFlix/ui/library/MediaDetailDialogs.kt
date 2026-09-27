package com.example.lingoFlix.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.theme.LingoSpacing

/**
 * Which modal the detail screen currently shows.
 *
 * One sealed value rather than a boolean per dialog, so two modals can never be
 * open at the same time.
 */
@Immutable
sealed interface DetailDialog {
    data object None : DetailDialog
    data object Rename : DetailDialog
    data object Delete : DetailDialog
    data class SyncOffset(val trackId: Long) : DetailDialog
    data class DeleteTrack(val trackId: Long) : DetailDialog
}

/** Renders whichever modal [dialog] selects. */
@Composable
fun MediaDetailDialogHost(
    dialog: DetailDialog,
    uiState: MediaDetailUiState,
    actions: MediaDetailActions,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        DetailDialog.None -> Unit

        DetailDialog.Rename -> RenameDialog(
            initial = uiState.item?.title.orEmpty(),
            onConfirm = {
                actions.onRename(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        DetailDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.library_delete_item),
            body = stringResource(R.string.detail_delete_media_body),
            onConfirm = {
                actions.onDelete()
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is DetailDialog.SyncOffset -> SyncOffsetDialog(
            initialOffsetMs = uiState.tracks
                .firstOrNull { it.id == dialog.trackId }
                ?.offsetMs
                ?: 0L,
            onConfirm = {
                actions.onSetOffset(dialog.trackId, it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is DetailDialog.DeleteTrack -> ConfirmDialog(
            title = stringResource(R.string.detail_delete_subtitle),
            body = stringResource(R.string.detail_delete_subtitle_body),
            onConfirm = {
                actions.onDeleteTrack(dialog.trackId)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

/**
 * Nudges every cue of a track earlier or later.
 *
 * Steps of 100 ms because that is roughly the smallest shift a viewer can
 * perceive, and finer control would turn a two-tap fix into a chore. The offset
 * is only committed on confirm, so the dialog can be abandoned safely.
 */
@Composable
private fun SyncOffsetDialog(
    initialOffsetMs: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var offset by remember(initialOffsetMs) { mutableLongStateOf(initialOffsetMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_sync_offset)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        R.string.detail_sync_offset_value,
                        formatOffset(offset),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = { offset -= OFFSET_STEP_MS }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.detail_sync_earlier),
                        )
                    }
                    TextButton(onClick = { offset = 0L }) {
                        Text(stringResource(R.string.detail_sync_reset))
                    }
                    FilledTonalIconButton(onClick = { offset += OFFSET_STEP_MS }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.detail_sync_later),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.detail_sync_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(offset) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_rename_item)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.library_item_name_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
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

/** Smallest shift a viewer reliably notices; finer steps would be busywork. */
private const val OFFSET_STEP_MS = 100L
