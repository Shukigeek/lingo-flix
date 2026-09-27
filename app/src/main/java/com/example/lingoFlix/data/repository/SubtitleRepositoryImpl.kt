package com.example.lingoFlix.data.repository

import android.content.Context
import android.net.Uri
import com.example.lingoFlix.core.di.IoDispatcher
import com.example.lingoFlix.data.local.db.dao.SentenceDao
import com.example.lingoFlix.data.local.db.dao.SubtitleTrackDao
import com.example.lingoFlix.data.local.db.entity.SentenceEntity
import com.example.lingoFlix.data.local.db.entity.SubtitleTrackEntity
import com.example.lingoFlix.data.mapper.toDomain
import com.example.lingoFlix.data.subtitle.ParsedCue
import com.example.lingoFlix.data.subtitle.SubtitleParser
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.domain.repository.SubtitleRepository
import com.example.lingoFlix.utils.LingoLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subtitle tracks and their parsed sentences.
 *
 * Subtitle files are copied into app storage on attach. A picked `content://`
 * URI can be revoked or point at a file the user later deletes, and losing the
 * subtitles would silently destroy every favourite that references them.
 */
@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: SubtitleTrackDao,
    private val sentenceDao: SentenceDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SubtitleRepository {

    override fun observeTracks(mediaItemId: Long): Flow<List<SubtitleTrack>> =
        trackDao.observeForMedia(mediaItemId).map { tracks -> tracks.map { it.toDomain() } }

    override suspend fun attachSubtitle(
        mediaItemId: Long,
        uri: String,
        language: String?,
        source: SubtitleSourceType,
        makePrimary: Boolean,
    ): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val bytes = readBytes(Uri.parse(uri))
            check(bytes.isNotEmpty()) { "Subtitle file is empty or unreadable: $uri" }

            val cues = SubtitleParser.parse(bytes)
            check(cues.isNotEmpty()) { "No subtitle cues found in $uri" }

            val stored = storeCopy(mediaItemId, uri, bytes)
            val trackId = trackDao.insert(
                SubtitleTrackEntity(
                    mediaItemId = mediaItemId,
                    language = language?.takeIf { it.isNotBlank() } ?: detectLanguage(cues),
                    filePath = stored.absolutePath,
                    source = source.name,
                    isPrimary = makePrimary,
                    sentenceCount = cues.size,
                )
            )
            persistCues(trackId, cues)
            if (makePrimary) trackDao.makePrimary(mediaItemId, trackId)
            trackId
        }.onFailure { LingoLog.w(TAG, "attachSubtitle failed for $uri: ${it.message}") }
    }

    override suspend fun deleteTrack(trackId: Long) = withContext(ioDispatcher) {
        // The stored copy is removed too; sentences go with the FK cascade.
        trackDao.findById(trackId)?.let { runCatching { File(it.filePath).delete() } }
        trackDao.deleteById(trackId)
    }

    override suspend fun setPrimaryTrack(mediaItemId: Long, trackId: Long) = withContext(ioDispatcher) {
        trackDao.makePrimary(mediaItemId, trackId)
    }

    override suspend fun setOffset(trackId: Long, offsetMs: Long) = withContext(ioDispatcher) {
        // Stored as a track property rather than rewriting every cue, so the
        // user can keep nudging the sync without cumulative rounding drift.
        trackDao.setOffset(trackId, offsetMs)
    }

    override suspend fun reparse(trackId: Long): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            val track = requireNotNull(trackDao.findById(trackId)) { "Unknown track $trackId" }
            val file = File(track.filePath)
            check(file.exists()) { "Subtitle file is gone: ${track.filePath}" }

            val cues = SubtitleParser.parse(file.readBytes())
            persistCues(trackId, cues)
            cues.size
        }.onFailure { LingoLog.w(TAG, "reparse failed for track $trackId: ${it.message}") }
    }

    // ── Internals ───────────────────────────────────────────────────────

    /** Replaces the track's sentences and keeps the denormalised count in step. */
    private suspend fun persistCues(trackId: Long, cues: List<ParsedCue>) {
        sentenceDao.replaceForTrack(
            trackId,
            cues.map { cue ->
                SentenceEntity(
                    subtitleTrackId = trackId,
                    indexInTrack = cue.index,
                    startMs = cue.startMs,
                    endMs = cue.endMs,
                    text = cue.text,
                    wordCount = cue.wordCount,
                )
            },
        )
        trackDao.setSentenceCount(trackId, cues.size)
    }

    private fun readBytes(uri: Uri): ByteArray = when (uri.scheme) {
        null, "file" -> File(requireNotNull(uri.path) { "File URI without a path" }).readBytes()
        else -> requireNotNull(context.contentResolver.openInputStream(uri)) {
            "Cannot open $uri"
        }.use { it.readBytes() }
    }

    /** Copies the file into `filesDir/subtitles`, keyed by media item and source URI. */
    private fun storeCopy(mediaItemId: Long, uri: String, bytes: ByteArray): File {
        val dir = File(context.filesDir, "subtitles").apply { mkdirs() }
        val extension = uri.substringAfterLast('.', "srt")
            .lowercase()
            .takeIf { it.length in 2..4 && it.all { c -> c in 'a'..'z' } }
            ?: "srt"
        val target = File(dir, "${mediaItemId}_${MediaProbe.hash(uri).take(16)}.$extension")
        target.writeBytes(bytes)
        return target
    }

    companion object {
        private const val TAG = "SubtitleRepository"

        /** Cues sampled for script detection; enough to be decisive, cheap to scan. */
        private const val LANGUAGE_SAMPLE_CUES = 40

        /**
         * Guesses the subtitle language from the script its characters belong to.
         *
         * Script detection is all that is needed here: the app only uses the
         * language to label tracks and pick a translation source, and a full
         * language identifier would be a heavy dependency for that.
         */
        fun detectLanguage(cues: List<ParsedCue>): String {
            val sample = cues.take(LANGUAGE_SAMPLE_CUES).joinToString(" ") { it.text }
            var hebrew = 0
            var arabic = 0
            var cyrillic = 0
            var han = 0
            var kana = 0
            for (ch in sample) {
                when (ch.code) {
                    in 0x0590..0x05FF -> hebrew++
                    in 0x0600..0x06FF -> arabic++
                    in 0x0400..0x04FF -> cyrillic++
                    in 0x3040..0x30FF -> kana++
                    in 0x4E00..0x9FFF -> han++
                }
            }
            return when {
                // Kana is checked before Han because Japanese text mixes both.
                kana > 0 && kana * 4 >= han -> "ja"
                han > 0 -> "zh"
                hebrew > arabic && hebrew > cyrillic && hebrew > 0 -> "he"
                arabic > cyrillic && arabic > 0 -> "ar"
                cyrillic > 0 -> "ru"
                else -> "en"
            }
        }
    }
}
