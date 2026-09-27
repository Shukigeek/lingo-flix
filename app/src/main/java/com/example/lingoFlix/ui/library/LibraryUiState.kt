package com.example.lingoFlix.ui.library

import androidx.compose.runtime.Immutable
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.domain.model.MediaItem

/**
 * Everything the library screen renders, in one immutable snapshot.
 *
 * The list in [items] is already sorted and filtered by the ViewModel. Keeping
 * that work out of composition means a recomposition never re-sorts a thousand
 * rows, and the screen stays a pure function of this object, which is what makes
 * the previews in LibraryPreviews.kt possible without Hilt or Room.
 */
@Immutable
data class LibraryUiState(
    val folderId: Long? = null,
    val folderName: String? = null,
    val breadcrumbs: List<LibraryFolder> = emptyList(),
    val folders: List<LibraryFolder> = emptyList(),
    val items: List<MediaItem> = emptyList(),
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOrder: SortOrder = SortOrder.TITLE,
    val filter: LibraryFilter = LibraryFilter(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val importMessage: String? = null,
    val selection: Set<Long> = emptySet(),
) {

    /** True while at least one item is ticked; the top bar turns into a selection bar. */
    val isSelectionMode: Boolean get() = selection.isNotEmpty()

    /** Root has no folder row to go back to, so the back arrow is hidden there. */
    val isRoot: Boolean get() = folderId == null

    /** Breadcrumbs only earn their vertical space once the user is nested. */
    val showBreadcrumbs: Boolean get() = breadcrumbs.isNotEmpty()

    private val hasContent: Boolean get() = folders.isNotEmpty() || items.isNotEmpty()

    /**
     * Nothing matched the active search or filter.
     *
     * Deliberately distinct from [isEmpty]: telling a user with a full library
     * that "the library is empty" because they typed a typo is the single most
     * confusing thing a browse screen can do.
     */
    val isFilteredEmpty: Boolean get() = !isLoading && !hasContent && filter.isActive

    /** Genuinely nothing here, so the empty state should offer an import. */
    val isEmpty: Boolean get() = !isLoading && !hasContent && !filter.isActive
}

/** Grid is the default: artwork is the fastest way to recognise a film. */
enum class ViewMode { GRID, LIST }

enum class SortOrder { TITLE, RECENTLY_ADDED, RECENTLY_WATCHED }

/**
 * User-controlled narrowing of the current folder's contents.
 *
 * [showWithoutSubtitles] defaults to true because hiding un-subtitled files by
 * default would make a fresh import look like it silently failed.
 */
@Immutable
data class LibraryFilter(
    val showWithoutSubtitles: Boolean = true,
    val onlyUnwatched: Boolean = false,
    val query: String = "",
) {
    /** Whether the user narrowed anything, which decides which empty state shows. */
    val isActive: Boolean
        get() = !showWithoutSubtitles || onlyUnwatched || query.isNotBlank()
}

/**
 * Every callback the library screen fires.
 *
 * Bundled into one stable holder so adding an action later does not touch every
 * call site, and so previews can pass a single no-op instance. Navigation
 * lambdas are wired by the NavHost; import lambdas by [LibraryRoute], which owns
 * the SAF launchers. The ViewModel never sees this type.
 */
@Immutable
data class LibraryActions(
    val onOpenFolder: (folderId: Long) -> Unit = {},
    val onOpenRoot: () -> Unit = {},
    val onOpenMedia: (mediaId: Long) -> Unit = {},
    val onBack: () -> Unit = {},
    /** Opens the system file picker; the result goes straight to the ViewModel. */
    val onPickVideos: () -> Unit = {},
    /** Opens the system directory picker for a recursive scan. */
    val onPickFolder: () -> Unit = {},
    val onCreateFolder: (name: String) -> Unit = {},
    val onRenameFolder: (folderId: Long, name: String) -> Unit = { _, _ -> },
    val onDeleteFolder: (folderId: Long) -> Unit = {},
    val onRenameItem: (mediaId: Long, title: String) -> Unit = { _, _ -> },
    val onDeleteItem: (mediaId: Long) -> Unit = {},
    val onMoveSelectionTo: (folderId: Long?) -> Unit = {},
    val onDeleteSelection: () -> Unit = {},
    val onToggleSelection: (mediaId: Long) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onSetViewMode: (ViewMode) -> Unit = {},
    val onSetSortOrder: (SortOrder) -> Unit = {},
    val onSetFilter: (LibraryFilter) -> Unit = {},
    val onSetQuery: (String) -> Unit = {},
    val onPruneMissing: () -> Unit = {},
    val onConsumeMessage: () -> Unit = {},
)
