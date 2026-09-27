package com.example.lingoFlix.ui.library

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs both library destinations.
 *
 * `Route.LibraryRoot` and `Route.Library(folderId)` share this ViewModel: the
 * only difference between them is whether a folder id is present in
 * [SavedStateHandle], so duplicating the whole screen for the root case would
 * buy nothing.
 *
 * Sorting and filtering happen here rather than in composition, so a
 * recomposition never re-sorts the list and the screen stays a pure function of
 * [LibraryUiState].
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The folder being browsed; `null` is the library root.
     *
     * Read by argument name rather than `toRoute<Route.Library>()` because the
     * same ViewModel also serves `Route.LibraryRoot`, which carries no argument
     * at all and would make a typed conversion throw.
     */
    private val folderId: Long? = savedStateHandle.get<Long>(ARG_FOLDER_ID)

    private val viewMode = MutableStateFlow(ViewMode.GRID)
    private val sortOrder = MutableStateFlow(SortOrder.TITLE)
    private val filter = MutableStateFlow(LibraryFilter())
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private val importing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val controls: Flow<Controls> =
        combine(viewMode, sortOrder, filter, selection, ::Controls)

    private val transient: Flow<Transient> = combine(importing, message, ::Transient)

    val uiState: StateFlow<LibraryUiState> = combine(
        folderTree(parentId = null, depth = 0),
        repository.observeItems(folderId),
        controls,
        transient,
    ) { tree, items, controls, transient ->
        val current = tree.firstOrNull { it.id == folderId }
        LibraryUiState(
            folderId = folderId,
            folderName = current?.name,
            breadcrumbs = if (folderId == null) emptyList() else ancestorsOf(tree, folderId),
            folders = tree
                .filter { it.parentId == folderId }
                .filter { matchesQuery(it, controls.filter.query) }
                .sortedBy { it.name.lowercase() },
            items = items.filtered(controls.filter).sorted(controls.sortOrder),
            viewMode = controls.viewMode,
            sortOrder = controls.sortOrder,
            filter = controls.filter,
            isLoading = false,
            isImporting = transient.isImporting,
            importMessage = transient.message,
            // Items can disappear under the user (a prune, a folder move), so
            // the selection is intersected with what is actually on screen.
            selection = controls.selection intersect items.map { it.id }.toSet(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LibraryUiState(folderId = folderId),
    )

    // ── Import ──────────────────────────────────────────────────────────

    /**
     * Imports every picked video into the folder being browsed.
     *
     * Files are imported one by one rather than in a batch so a single
     * unreadable URI cannot abort the rest of the selection.
     */
    fun importVideos(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            var imported = 0
            var failed = 0
            uris.forEach { uri ->
                repository.importVideo(uri, folderId)
                    .onSuccess { imported++ }
                    .onFailure { failed++ }
            }
            importing.value = false
            message.value = context.getString(R.string.library_import_files_result, imported, failed)
        }
    }

    fun importFolder(treeUri: String) {
        viewModelScope.launch {
            importing.value = true
            val result = repository.importFolder(treeUri, folderId)
            importing.value = false
            message.value = result.fold(
                onSuccess = {
                    context.getString(
                        R.string.library_import_folder_result,
                        it.imported,
                        it.skipped,
                        it.subtitlesMatched,
                    )
                },
                onFailure = { context.getString(R.string.library_import_failed) },
            )
        }
    }

    fun pruneMissing() {
        viewModelScope.launch {
            val removed = repository.pruneMissingFiles()
            message.value = if (removed > 0) {
                context.getString(R.string.library_prune_result, removed)
            } else {
                context.getString(R.string.library_prune_none)
            }
        }
    }

    /** Clears the snackbar message so it is not shown again after a rotation. */
    fun consumeMessage() {
        message.value = null
    }

    // ── Folders ─────────────────────────────────────────────────────────

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createFolder(folderId, name) }
    }

    fun renameFolder(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameFolder(id, name) }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch { repository.deleteFolder(id) }
    }

    // ── Items ───────────────────────────────────────────────────────────

    fun renameItem(id: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.renameItem(id, title) }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
            selection.value -= id
        }
    }

    fun moveSelectionTo(folderId: Long?) {
        val ids = selection.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveItems(ids, folderId)
            selection.value = emptySet()
        }
    }

    /**
     * Removes every selected item.
     *
     * Composed from single deletes because the repository exposes no bulk
     * delete; the selection is capped by what fits on screen, so the loop is not
     * a practical concern.
     */
    fun deleteSelection() {
        val ids = selection.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.deleteItem(it) }
            selection.value = emptySet()
        }
    }

    fun toggleSelection(id: Long) {
        val current = selection.value
        selection.value = if (id in current) current - id else current + id
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    // ── View options ────────────────────────────────────────────────────

    fun setViewMode(mode: ViewMode) {
        viewMode.value = mode
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    fun setQuery(query: String) {
        filter.value = filter.value.copy(query = query)
    }

    // ── Derivation helpers ──────────────────────────────────────────────

    private fun List<MediaItem>.filtered(filter: LibraryFilter): List<MediaItem> {
        val query = filter.query.trim()
        return filter { item ->
            val subtitlesOk = filter.showWithoutSubtitles || item.hasSubtitles
            val watchedOk = !filter.onlyUnwatched || item.watchProgress?.completed != true
            val queryOk = query.isEmpty() || item.matchesQuery(query)
            subtitlesOk && watchedOk && queryOk
        }
    }

    private fun MediaItem.matchesQuery(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
            displayTitle.contains(query, ignoreCase = true) ||
            seriesName?.contains(query, ignoreCase = true) == true

    private fun matchesQuery(folder: LibraryFolder, query: String): Boolean =
        query.isBlank() || folder.name.contains(query.trim(), ignoreCase = true)

    /**
     * Ordered newest-first by row id for [SortOrder.RECENTLY_ADDED].
     *
     * The domain model carries no "added at" timestamp, and the repository
     * contract may not be changed here, so the auto-incrementing primary key is
     * used as the insertion order proxy.
     */
    private fun List<MediaItem>.sorted(order: SortOrder): List<MediaItem> = when (order) {
        SortOrder.TITLE -> sortedBy { it.displayTitle.lowercase() }
        SortOrder.RECENTLY_ADDED -> sortedByDescending { it.id }
        SortOrder.RECENTLY_WATCHED -> sortedByDescending { it.watchProgress?.updatedAt ?: 0L }
    }

    /**
     * Rebuilds the whole folder tree from the root downwards.
     *
     * [LibraryRepository] can only observe the children of one parent, so there
     * is no way to look a folder up by id or to walk upwards to build a
     * breadcrumb trail. Expanding the tree once gives both the current folder's
     * name and its ancestors from the same emission. Depth is capped so a
     * corrupt parent cycle cannot spin forever.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun folderTree(parentId: Long?, depth: Int): Flow<List<LibraryFolder>> =
        repository.observeFolders(parentId).flatMapLatest { children ->
            if (children.isEmpty() || depth >= MAX_TREE_DEPTH) {
                flowOf(children)
            } else {
                combine(children.map { folderTree(it.id, depth + 1) }) { branches ->
                    children + branches.toList().flatten()
                }
            }
        }

    /** Root-first chain of folders leading to [id], inclusive. */
    private fun ancestorsOf(tree: List<LibraryFolder>, id: Long): List<LibraryFolder> {
        val byId = tree.associateBy { it.id }
        val trail = ArrayDeque<LibraryFolder>()
        var cursor = byId[id]
        var guard = 0
        while (cursor != null && guard++ < MAX_TREE_DEPTH) {
            trail.addFirst(cursor)
            cursor = cursor.parentId?.let { byId[it] }
        }
        return trail.toList()
    }

    private data class Controls(
        val viewMode: ViewMode,
        val sortOrder: SortOrder,
        val filter: LibraryFilter,
        val selection: Set<Long>,
    )

    private data class Transient(val isImporting: Boolean, val message: String?)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /** Guards against a parent cycle and pathological nesting. */
        const val MAX_TREE_DEPTH = 8

        /** Matches the property name of `Route.Library.folderId`. */
        const val ARG_FOLDER_ID = "folderId"
    }
}
