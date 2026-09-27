package com.example.lingoFlix.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.lingoFlix.data.local.db.dao.LibraryFolderDao
import com.example.lingoFlix.data.local.db.dao.MediaItemDao
import com.example.lingoFlix.data.local.db.entity.FolderKind
import com.example.lingoFlix.data.local.db.entity.MediaItemEntity
import com.example.lingoFlix.data.local.db.entity.MediaKind
import com.example.lingoFlix.data.local.db.entity.MediaSourceType
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.domain.repository.ImportSummary
import com.example.lingoFlix.domain.repository.SubtitleRepository
import com.example.lingoFlix.util.MediaNameParser
import com.example.lingoFlix.util.ParsedMediaName
import com.example.lingoFlix.utils.LingoLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of importing one file, so the caller can build an [ImportSummary]. */
data class ImportOutcome(
    val mediaItemId: Long,
    val alreadyPresent: Boolean,
    val subtitleAttached: Boolean,
)

/**
 * Turns picked URIs into library rows.
 *
 * Kept out of `LibraryRepositoryImpl` because importing is where almost all of
 * the Android-specific complexity lives (SAF permissions, metadata extraction,
 * folder inference, subtitle matching), and the repository should stay a thin,
 * readable orchestration layer over the DAOs.
 *
 * The caller is responsible for the dispatcher: every method here blocks.
 */
@Singleton
class MediaImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaItemDao: MediaItemDao,
    private val folderDao: LibraryFolderDao,
    private val subtitleRepository: SubtitleRepository,
) {

    /**
     * Imports a single video.
     *
     * @param subtitleUri an already-discovered sibling subtitle; when `null` the
     *   importer looks for one next to the video itself.
     * @param parentFolderNames folder context used by [MediaNameParser] to
     *   recover a series name the file name alone does not carry.
     */
    suspend fun importVideo(
        uri: String,
        folderId: Long?,
        subtitleUri: String? = null,
        parentFolderNames: List<String> = emptyList(),
    ): ImportOutcome {
        val parsedUri = Uri.parse(uri)
        takePersistablePermission(parsedUri)

        mediaItemDao.findByUri(uri)?.let {
            return ImportOutcome(it.id, alreadyPresent = true, subtitleAttached = false)
        }

        val info = MediaProbe.documentInfo(context, parsedUri)
        val fileName = info.displayName ?: parsedUri.lastPathSegment.orEmpty()
        val parsed = MediaNameParser.parse(fileName, parentFolderNames)
        val probe = MediaProbe.probeVideo(context, parsedUri)

        val targetFolderId = folderId ?: resolveFolder(parsed)
        val insertedId = mediaItemDao.insert(
            MediaItemEntity(
                folderId = targetFolderId,
                title = parsed.title.ifBlank { fileName },
                fileUri = uri,
                sourceType = MediaSourceType.LOCAL,
                kind = if (parsed.isEpisode) MediaKind.EPISODE else MediaKind.MOVIE,
                seriesName = parsed.seriesName,
                season = parsed.season,
                episode = parsed.episode,
                durationMs = probe.durationMs,
                thumbnailPath = probe.thumbnailPath,
                sizeBytes = info.sizeBytes,
            )
        )

        // insert() ignores conflicts, so -1 means a concurrent import won the race.
        if (insertedId <= 0) {
            val existing = mediaItemDao.findByUri(uri)
            return ImportOutcome(existing?.id ?: -1, alreadyPresent = true, subtitleAttached = false)
        }

        val subtitle = subtitleUri ?: findSiblingSubtitle(parsedUri, fileName)
        val attached = subtitle != null && subtitleRepository
            .attachSubtitle(insertedId, subtitle, source = SubtitleSourceType.AUTO_MATCH)
            .isSuccess

        return ImportOutcome(insertedId, alreadyPresent = false, subtitleAttached = attached)
    }

    /** Walks a picked tree and imports every supported video beneath it. */
    suspend fun importFolder(treeUri: String, parentFolderId: Long?): ImportSummary {
        val rootUri = Uri.parse(treeUri)
        takePersistablePermission(rootUri)

        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: return ImportSummary(imported = 0, skipped = 0, subtitlesMatched = 0, failed = 0)

        val found = mutableListOf<DiscoveredVideo>()
        collect(root, listOf(root.name.orEmpty()).filter { it.isNotBlank() }, found, depth = 0)

        var imported = 0
        var skipped = 0
        var matched = 0
        var failed = 0
        for (video in found) {
            try {
                val outcome = importVideo(
                    uri = video.uri,
                    folderId = parentFolderId,
                    subtitleUri = video.subtitleUri,
                    parentFolderNames = video.parents,
                )
                if (outcome.alreadyPresent) skipped++ else imported++
                if (outcome.subtitleAttached) matched++
            } catch (e: Exception) {
                LingoLog.w(TAG, "Import failed for ${video.uri}: ${e.message}")
                failed++
            }
        }
        return ImportSummary(imported, skipped, matched, failed)
    }

    // ── Discovery ───────────────────────────────────────────────────────

    private data class DiscoveredVideo(
        val uri: String,
        val parents: List<String>,
        val subtitleUri: String?,
    )

    /**
     * Collects videos and subtitles in a single pass per directory so sibling
     * subtitles can be matched without a second traversal of the tree.
     */
    private fun collect(
        dir: DocumentFile,
        parents: List<String>,
        into: MutableList<DiscoveredVideo>,
        depth: Int,
    ) {
        if (depth > MAX_DEPTH) return
        val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())

        val subtitles = children
            .filter { it.isFile && extensionOf(it.name) in SUBTITLE_EXTENSIONS }
            .associateBy { baseNameOf(it.name).lowercase() }

        for (child in children) {
            val name = child.name.orEmpty()
            when {
                child.isDirectory -> collect(child, parents + name, into, depth + 1)
                extensionOf(name) in VIDEO_EXTENSIONS -> {
                    val base = baseNameOf(name).lowercase()
                    val subtitle = subtitles[base]
                        ?: subtitles.entries.firstOrNull { it.key.startsWith(base) }?.value
                    into += DiscoveredVideo(
                        uri = child.uri.toString(),
                        parents = parents,
                        subtitleUri = subtitle?.uri?.toString(),
                    )
                }
            }
        }
    }

    /**
     * Looks for a subtitle next to a single picked video.
     *
     * Only possible for tree-backed documents and plain files: a one-shot
     * `content://` pick grants no access to the containing directory.
     */
    private fun findSiblingSubtitle(videoUri: Uri, fileName: String): String? = runCatching {
        val parent = DocumentFile.fromSingleUri(context, videoUri)?.parentFile
            ?: videoUri.path
                ?.takeIf { videoUri.scheme == null || videoUri.scheme == "file" }
                ?.let { File(it).parentFile }
                ?.let { DocumentFile.fromFile(it) }
            ?: return null

        val base = baseNameOf(fileName).lowercase()
        parent.listFiles()
            .firstOrNull {
                it.isFile &&
                    extensionOf(it.name) in SUBTITLE_EXTENSIONS &&
                    baseNameOf(it.name).lowercase() == base
            }
            ?.uri
            ?.toString()
    }.getOrNull()

    // ── Folders ─────────────────────────────────────────────────────────

    /**
     * Creates `Series/Season NN` for episodes so a bulk import lands in a
     * browsable tree instead of one flat list. Films stay at the root, where
     * the user's own folders live.
     */
    private suspend fun resolveFolder(parsed: ParsedMediaName): Long? {
        val series = parsed.seriesName?.takeIf { parsed.isEpisode && it.isNotBlank() } ?: return null
        val seriesFolderId = folderDao.findOrCreate(null, series, FolderKind.SERIES)
        val season = parsed.season ?: return seriesFolderId
        return folderDao.findOrCreate(seriesFolderId, "Season %02d".format(season), FolderKind.SEASON)
    }

    // ── SAF ─────────────────────────────────────────────────────────────

    /**
     * Keeps read access across process death. Providers may refuse, and a
     * refusal is not fatal: the URI usually still works for this session.
     */
    private fun takePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            LingoLog.w(TAG, "No persistable permission for $uri: ${e.message}")
        } catch (e: Exception) {
            LingoLog.w(TAG, "takePersistableUriPermission failed for $uri: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "MediaImporter"

        /** Guards against symlink loops and pathological directory nesting. */
        private const val MAX_DEPTH = 12

        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "webm", "m4v", "flv", "wmv", "mpg", "mpeg", "ts",
        )

        val SUBTITLE_EXTENSIONS = setOf("srt", "vtt")

        fun extensionOf(name: String?): String =
            name.orEmpty().substringAfterLast('.', "").lowercase()

        fun baseNameOf(name: String?): String {
            val value = name.orEmpty()
            val dot = value.lastIndexOf('.')
            return if (dot > 0) value.substring(0, dot) else value
        }
    }
}
