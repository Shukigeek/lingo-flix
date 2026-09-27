package com.example.lingoFlix.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.EmptyState
import com.example.lingoFlix.ui.components.LingoScaffold
import com.example.lingoFlix.ui.components.LoadingState
import com.example.lingoFlix.ui.theme.LingoSpacing

/**
 * Stateful entry point for both library destinations.
 *
 * Owns the SAF launchers because `rememberLauncherForActivityResult` needs an
 * `ActivityResultRegistryOwner`, which does not exist in a Compose preview;
 * keeping them here is what lets [LibraryScreen] stay previewable. Picked URIs
 * are handed to the ViewModel as plain strings so no Android type crosses into
 * the state layer.
 */
@Composable
fun LibraryRoute(
    onOpenFolder: (Long) -> Unit,
    onOpenMedia: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenRoot: () -> Unit = onBack,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickVideos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.importVideos(uris.map { it.toString() }) }

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.importFolder(it.toString()) } }

    val actions = remember(viewModel, onOpenFolder, onOpenMedia, onBack, onOpenRoot) {
        LibraryActions(
            onOpenFolder = onOpenFolder,
            onOpenRoot = onOpenRoot,
            onOpenMedia = onOpenMedia,
            onBack = onBack,
            onPickVideos = { pickVideos.launch(arrayOf(VIDEO_MIME_TYPE)) },
            onPickFolder = { pickFolder.launch(null) },
            onCreateFolder = viewModel::createFolder,
            onRenameFolder = viewModel::renameFolder,
            onDeleteFolder = viewModel::deleteFolder,
            onRenameItem = viewModel::renameItem,
            onDeleteItem = viewModel::deleteItem,
            onMoveSelectionTo = viewModel::moveSelectionTo,
            onDeleteSelection = viewModel::deleteSelection,
            onToggleSelection = viewModel::toggleSelection,
            onClearSelection = viewModel::clearSelection,
            onSetViewMode = viewModel::setViewMode,
            onSetSortOrder = viewModel::setSortOrder,
            onSetFilter = viewModel::setFilter,
            onSetQuery = viewModel::setQuery,
            onPruneMissing = viewModel::pruneMissing,
            onConsumeMessage = viewModel::consumeMessage,
        )
    }

    LibraryScreen(uiState = uiState, actions = actions, modifier = modifier)
}

/**
 * Folder browser for the user's imported media.
 *
 * Fully stateless apart from two purely visual toggles (search field visibility
 * and which dialog is open), which have no meaning outside composition and would
 * only bloat [LibraryUiState].
 */
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
) {
    var searchVisible by rememberSaveable { mutableStateOf(value = false) }
    var dialog by remember { mutableStateOf<LibraryDialog>(LibraryDialog.None) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.importMessage) {
        val text = uiState.importMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        actions.onConsumeMessage()
    }

    LibraryDialogHost(
        dialog = dialog,
        uiState = uiState,
        actions = actions,
        onDismiss = { dialog = LibraryDialog.None },
    )

    LingoScaffold(
        modifier = modifier,
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    count = uiState.selection.size,
                    onClear = actions.onClearSelection,
                    onMove = { dialog = LibraryDialog.MoveSelection },
                    onDelete = { dialog = LibraryDialog.DeleteSelection },
                )
            } else {
                LibraryTopBar(
                    title = uiState.folderName ?: stringResource(R.string.library_title),
                    onBack = if (uiState.isRoot) null else actions.onBack,
                    searchVisible = searchVisible,
                    uiState = uiState,
                    actions = actions,
                    onToggleSearch = {
                        searchVisible = !searchVisible
                        // Closing search must also drop the query, or the list
                        // would stay filtered by an input the user cannot see.
                        if (!searchVisible) actions.onSetQuery("")
                    },
                    onNewFolder = { dialog = LibraryDialog.CreateFolder },
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) AddMediaFab(actions = actions)
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isImporting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (searchVisible) {
                    LibrarySearchField(
                        query = uiState.filter.query,
                        onQueryChange = actions.onSetQuery,
                        onClose = {
                            searchVisible = false
                            actions.onSetQuery("")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LingoSpacing.md, vertical = LingoSpacing.sm),
                    )
                }
                if (uiState.showBreadcrumbs) {
                    BreadcrumbRow(
                        breadcrumbs = uiState.breadcrumbs,
                        onOpenRoot = actions.onOpenRoot,
                        onOpenFolder = actions.onOpenFolder,
                    )
                }
                LibraryBody(
                    uiState = uiState,
                    actions = actions,
                    onFolderMenu = { dialog = LibraryDialog.FolderOptions(it) },
                    onItemMenu = { dialog = LibraryDialog.ItemOptions(it) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Hosted inside the content rather than via a Scaffold slot, so the
            // snackbar can float clear of the FAB without changing the shared
            // LingoScaffold contract.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SNACKBAR_BOTTOM_INSET),
            )
        }
    }
}

/**
 * Chooses between the mutually exclusive body states.
 *
 * Empty-because-nothing-imported and empty-because-of-a-filter are different
 * problems with different fixes, so they get different copy and different
 * actions instead of one generic message.
 */
@Composable
private fun LibraryBody(
    uiState: LibraryUiState,
    actions: LibraryActions,
    onFolderMenu: (Long) -> Unit,
    onItemMenu: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> LoadingState(modifier = modifier)

        uiState.isFilteredEmpty -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.library_no_results_title),
                body = stringResource(R.string.library_no_results_body),
            )
        }

        uiState.isEmpty && !uiState.isRoot -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.library_folder_empty_title),
                body = stringResource(R.string.library_folder_empty_body),
                actionLabel = stringResource(R.string.library_add_video),
                onAction = actions.onPickVideos,
            )
        }

        uiState.isEmpty -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Default.MovieFilter,
                title = stringResource(R.string.library_empty_title),
                body = stringResource(R.string.library_empty_body),
                actionLabel = stringResource(R.string.library_add_video),
                onAction = actions.onPickVideos,
            )
        }

        else -> LibraryContent(
            uiState = uiState,
            actions = actions,
            onFolderMenu = onFolderMenu,
            onItemMenu = onItemMenu,
            modifier = modifier,
        )
    }
}

/**
 * The SAF filter for video picking.
 *
 * Wide on purpose: narrowing further would hide `.mkv` files, which many
 * providers report with a non-standard MIME type.
 */
private const val VIDEO_MIME_TYPE = "video/*"

/** Clears the FAB so an import result is never hidden behind it. */
private val SNACKBAR_BOTTOM_INSET = LingoSpacing.xl * 2
