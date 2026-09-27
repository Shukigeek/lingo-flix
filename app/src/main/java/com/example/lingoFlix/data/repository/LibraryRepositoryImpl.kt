package com.example.lingoFlix.data.repository

import android.content.Context
import android.net.Uri
import com.example.lingoFlix.core.di.IoDispatcher
import com.example.lingoFlix.data.local.db.dao.LibraryFolderDao
import com.example.lingoFlix.data.local.db.dao.MediaItemDao
import com.example.lingoFlix.data.local.db.dao.MediaItemWithDetails
import com.example.lingoFlix.data.local.db.dao.WatchProgressDao
import com.example.lingoFlix.data.local.db.entity.FolderKind
import com.example.lingoFlix.data.local.db.entity.LOCAL_USER_ID
import com.example.lingoFlix.data.local.db.entity.WatchProgressEntity
import com.example.lingoFlix.data.mapper.toDomain
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.repository.ImportSummary
import com.example.lingoFlix.domain.repository.LibraryRepository
import com.example.lingoFlix.utils.LingoLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The library: folders, media items and resume positions.
 *
 * Reads are exposed as Room `Flow`s so the UI always reflects the database
 * without a manual refresh; writes are pushed onto the IO dispatcher because
 * Room's suspend DAOs still do real disk work.
 */
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderDao: LibraryFolderDao,
    private val mediaItemDao: MediaItemDao,
    private val watchProgressDao: WatchProgressDao,
    private val importer: MediaImporter,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LibraryRepository {

    // ── Reads ───────────────────────────────────────────────────────────

    /**
     * Folder counts are derived from the item list rather than a SQL count,
     * because the DAO contract offers no per-folder aggregate. The derived map
     * is de-duplicated so unrelated media changes (a resume position ticking
     * during playback) do not re-render the folder list.
     */
    override fun observeFolders(parentId: Long?): Flow<List<LibraryFolder>> {
        val counts = mediaItemDao.observeAll()
            .map { items -> items.groupingBy { it.item.folderId }.eachCount() }
            .distinctUntilChanged()
        return combine(folderDao.observeChildren(parentId), counts) { folders, byFolder ->
            folders.map { it.toDomain(itemCount = byFolder[it.id] ?: 0) }
        }
    }

    override fun observeItems(folderId: Long?): Flow<List<MediaItem>> =
        mediaItemDao.observeByFolder(folderId).mapToDomain()

    override fun observeAllItems(): Flow<List<MediaItem>> =
        mediaItemDao.observeAll().mapToDomain()

    override fun observeItem(id: Long): Flow<MediaItem?> =
        mediaItemDao.observeById(id).map { it?.toDomain() }

    override fun observeContinueWatching(limit: Int): Flow<List<MediaItem>> =
        mediaItemDao.observeContinueWatching(LOCAL_USER_ID, limit).mapToDomain()

    override fun searchItems(query: String): Flow<List<MediaItem>> =
        mediaItemDao.search(query).mapToDomain()

    override suspend fun getItem(id: Long): MediaItem? = withContext(ioDispatcher) {
        mediaItemDao.findById(id)?.toDomain()
    }

    // ── Import ──────────────────────────────────────────────────────────

    override suspend fun importVideo(uri: String, folderId: Long?): Result<Long> =
        withContext(ioDispatcher) {
            runCatching { importer.importVideo(uri, folderId).mediaItemId }
                .onFailure { LingoLog.w(TAG, "importVideo failed for $uri: ${it.message}") }
        }

    override suspend fun importFolder(treeUri: String, parentFolderId: Long?): Result<ImportSummary> =
        withContext(ioDispatcher) {
            runCatching { importer.importFolder(treeUri, parentFolderId) }
                .onFailure { LingoLog.w(TAG, "importFolder failed for $treeUri: ${it.message}") }
        }

    // ── Mutations ───────────────────────────────────────────────────────

    override suspend fun createFolder(parentId: Long?, name: String): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val trimmed = name.trim()
                require(trimmed.isNotEmpty()) { "Folder name cannot be empty" }
                // findOrCreate keeps the operation idempotent, so a double tap
                // cannot produce two identically named siblings.
                folderDao.findOrCreate(parentId, trimmed, FolderKind.CUSTOM)
            }
        }

    override suspend fun renameFolder(id: Long, name: String) = withContext(ioDispatcher) {
        folderDao.rename(id, name.trim())
    }

    override suspend fun deleteFolder(id: Long) = withContext(ioDispatcher) {
        // Child folders cascade; items are detached by the FK's SET NULL rule
        // so deleting a folder never destroys the user's library.
        folderDao.deleteById(id)
    }

    override suspend fun renameItem(id: Long, title: String) = withContext(ioDispatcher) {
        mediaItemDao.rename(id, title.trim())
    }

    override suspend fun moveItems(ids: List<Long>, folderId: Long?) = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) mediaItemDao.moveToFolder(ids, folderId)
    }

    override suspend fun deleteItem(id: Long) = withContext(ioDispatcher) {
        mediaItemDao.deleteById(id)
    }

    override suspend fun pruneMissingFiles(): Int = withContext(ioDispatcher) {
        val missing = mediaItemDao.allUris().filterNot { MediaProbe.exists(context, Uri.parse(it)) }
        if (missing.isNotEmpty()) mediaItemDao.deleteByUris(missing)
        missing.size
    }

    override suspend fun saveProgress(
        mediaItemId: Long,
        positionMs: Long,
        completed: Boolean,
    ) = withContext(ioDispatcher) {
        watchProgressDao.upsert(
            WatchProgressEntity(
                userId = LOCAL_USER_ID,
                mediaItemId = mediaItemId,
                positionMs = positionMs.coerceAtLeast(0),
                completed = completed,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun Flow<List<MediaItemWithDetails>>.mapToDomain(): Flow<List<MediaItem>> =
        map { rows -> rows.map { it.toDomain() } }

    private companion object {
        const val TAG = "LibraryRepository"
    }
}
