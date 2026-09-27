package com.example.lingoFlix.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.ui.components.ErrorState
import com.example.lingoFlix.ui.components.LingoScaffold
import com.example.lingoFlix.ui.components.LoadingState
import com.example.lingoFlix.ui.components.PrimaryButton
import com.example.lingoFlix.ui.theme.LingoSpacing

/**
 * Stateful entry point for the media detail destination.
 *
 * Owns the subtitle picker for the same reason [LibraryRoute] owns its
 * launchers: `rememberLauncherForActivityResult` has no registry owner inside a
 * Compose preview, and [MediaDetailScreen] must stay previewable.
 */
@Composable
fun MediaDetailRoute(
    onBack: () -> Unit,
    onPlay: (mediaId: Long) -> Unit,
    onLearn: (mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickSubtitle = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.attachSubtitle(it.toString()) } }

    val actions = remember(viewModel, onBack, onPlay, onLearn) {
        MediaDetailActions(
            onBack = onBack,
            onPlay = onPlay,
            onLearn = onLearn,
            // Not narrowed to text/*: providers routinely report .srt as
            // application/octet-stream, which such a filter would hide.
            onPickSubtitle = { pickSubtitle.launch(arrayOf(ANY_MIME_TYPE)) },
            onMakePrimary = viewModel::makePrimary,
            onSetOffset = viewModel::setOffset,
            onDeleteTrack = viewModel::deleteTrack,
            onRename = viewModel::rename,
            onDelete = {
                viewModel.delete()
                onBack()
            },
            onConsumeMessage = viewModel::consumeMessage,
        )
    }

    MediaDetailScreen(uiState = uiState, actions = actions, modifier = modifier)
}

/**
 * Everything about one library item: artwork, metadata, and its subtitles.
 *
 * Stateless apart from the dialogs, which have no meaning outside composition.
 * The screen never decides where a tap leads; it reports through [actions].
 */
@Composable
fun MediaDetailScreen(
    uiState: MediaDetailUiState,
    actions: MediaDetailActions,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<DetailDialog>(DetailDialog.None) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val text = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        actions.onConsumeMessage()
    }

    MediaDetailDialogHost(
        dialog = dialog,
        uiState = uiState,
        actions = actions,
        onDismiss = { dialog = DetailDialog.None },
    )

    LingoScaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> LoadingState()

                uiState.item == null -> ErrorState(
                    message = stringResource(R.string.detail_not_found),
                    onRetry = actions.onBack,
                )

                else -> DetailContent(
                    item = uiState.item,
                    uiState = uiState,
                    actions = actions,
                    onOpenDialog = { dialog = it },
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = LingoSpacing.lg),
            )
        }
    }
}

@Composable
private fun DetailContent(
    item: MediaItem,
    uiState: MediaDetailUiState,
    actions: MediaDetailActions,
    onOpenDialog: (DetailDialog) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LingoSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(LingoSpacing.md),
    ) {
        item {
            DetailHeader(
                item = item,
                onBack = actions.onBack,
                onRename = { onOpenDialog(DetailDialog.Rename) },
                onDelete = { onOpenDialog(DetailDialog.Delete) },
            )
        }

        item { MetadataRow(item = item) }

        item.metadata?.overview?.takeIf { it.isNotBlank() }?.let { overview ->
            item { OverviewSection(overview = overview) }
        }

        item {
            PrimaryAction(
                item = item,
                hasSubtitles = uiState.hasSubtitles,
                onPlay = actions.onPlay,
                onLearn = actions.onLearn,
                onAttach = actions.onPickSubtitle,
            )
        }

        if (uiState.isAttaching) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LingoSpacing.md),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        subtitleSection(
            uiState = uiState,
            actions = actions,
            onOpenDialog = onOpenDialog,
        )
    }
}

/**
 * The one thing the user came here to do.
 *
 * Without subtitles there is nothing to learn from, so the wording drops to
 * plain playback and a secondary call to action points at the fix rather than
 * leaving the user to find the subtitle list further down the page.
 */
@Composable
private fun PrimaryAction(
    item: MediaItem,
    hasSubtitles: Boolean,
    onPlay: (Long) -> Unit,
    onLearn: (Long) -> Unit,
    onAttach: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(LingoSpacing.sm),
    ) {
        if (hasSubtitles) {
            PrimaryButton(
                text = stringResource(R.string.detail_learn),
                onClick = { onLearn(item.id) },
                icon = Icons.Default.School,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.detail_watch),
                onClick = { onPlay(item.id) },
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            AttachSubtitleCallout(onAttach = onAttach)
        }
    }
}

/** Broadest possible SAF filter; see [MediaDetailRoute] for why. */
private const val ANY_MIME_TYPE = "*/*"
