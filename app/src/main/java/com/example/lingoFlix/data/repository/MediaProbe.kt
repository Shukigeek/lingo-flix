package com.example.lingoFlix.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.lingoFlix.utils.LingoLog
import java.io.File
import java.security.MessageDigest

/** Display name and byte size of a picked document, as far as the provider reports them. */
internal data class DocumentInfo(val displayName: String?, val sizeBytes: Long)

/** Duration and poster frame extracted from a video file. */
internal data class VideoProbe(val durationMs: Long, val thumbnailPath: String?)

/**
 * Content-resolver and `MediaMetadataRetriever` plumbing used by the importer.
 *
 * Extracted from the repository so the import flow reads as business logic and
 * every Android failure mode (revoked permission, unreadable codec, provider
 * without the expected columns) is contained in one place. Nothing here throws:
 * an unreadable file still deserves a library row built from its name.
 */
internal object MediaProbe {

    private const val TAG = "MediaProbe"

    /** Poster frame taken a few seconds in, past logos and black leader frames. */
    private const val THUMBNAIL_POSITION_US = 5_000_000L

    private const val THUMBNAIL_QUALITY = 85

    /** Reads `DISPLAY_NAME` and `SIZE`, falling back to the URI's last path segment. */
    fun documentInfo(context: Context, uri: Uri): DocumentInfo {
        if (uri.scheme == "file") {
            val file = runCatching { File(requireNotNull(uri.path)) }.getOrNull()
            return DocumentInfo(file?.name ?: uri.lastPathSegment, file?.length() ?: 0L)
        }
        var name: String? = null
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }.onFailure { LingoLog.w(TAG, "Could not query $uri: ${it.message}") }

        val resolved = name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        return DocumentInfo(resolved, size)
    }

    /**
     * Extracts duration and writes a JPEG poster frame to `filesDir/thumbnails`.
     *
     * The retriever holds a native decoder, so it is always released; leaking it
     * across a folder import of hundreds of files would exhaust the codec pool.
     */
    fun probeVideo(context: Context, uri: Uri): VideoProbe {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val frame = runCatching {
                retriever.getFrameAtTime(
                    THUMBNAIL_POSITION_US.coerceAtMost(maxOf(durationMs * 1_000 / 2, 0L)),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                )
            }.getOrNull()
            VideoProbe(durationMs, frame?.let { saveThumbnail(context, uri.toString(), it) })
        } catch (e: Exception) {
            LingoLog.w(TAG, "Could not probe $uri: ${e.message}")
            VideoProbe(0L, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Stores [bitmap] under a hash of the source URI so re-importing the same
     * file reuses the file instead of growing the cache directory.
     */
    private fun saveThumbnail(context: Context, uriKey: String, bitmap: Bitmap): String? = runCatching {
        val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val target = File(dir, "${hash(uriKey)}.jpg")
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, it) }
        target.absolutePath
    }.onFailure { LingoLog.w(TAG, "Could not save thumbnail: ${it.message}") }.getOrNull()

    /** Short, filesystem-safe, stable identifier for an arbitrary URI string. */
    fun hash(value: String): String = runCatching {
        MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }.getOrElse { value.hashCode().toUInt().toString(16) }

    /** True when the URI can still be opened; used to prune deleted files. */
    fun exists(context: Context, uri: Uri): Boolean = runCatching {
        when (uri.scheme) {
            "file" -> uri.path?.let { File(it).exists() } == true
            else -> context.contentResolver.openInputStream(uri)?.use { true } == true
        }
    }.getOrDefault(false)
}
