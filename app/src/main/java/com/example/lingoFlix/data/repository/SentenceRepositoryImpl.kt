package com.example.lingoFlix.data.repository

import com.example.lingoFlix.core.di.IoDispatcher
import com.example.lingoFlix.data.local.db.dao.FavoriteSentenceDao
import com.example.lingoFlix.data.local.db.dao.SentenceDao
import com.example.lingoFlix.data.local.db.dao.SubtitleTrackDao
import com.example.lingoFlix.data.local.db.entity.FavoriteSentenceEntity
import com.example.lingoFlix.data.local.db.entity.LOCAL_USER_ID
import com.example.lingoFlix.data.mapper.toDomain
import com.example.lingoFlix.domain.model.SavedSentence
import com.example.lingoFlix.domain.model.Sentence
import com.example.lingoFlix.domain.repository.SentenceRepository
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sentences, their translations and the user's favourites.
 *
 * Every sentence leaves this layer with its track offset already applied, so
 * callers can seek to [Sentence.startMs] without knowing that manual subtitle
 * sync exists at all.
 */
@Singleton
class SentenceRepositoryImpl @Inject constructor(
    private val sentenceDao: SentenceDao,
    private val trackDao: SubtitleTrackDao,
    private val favoriteDao: FavoriteSentenceDao,
    private val translator: SentenceTranslator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SentenceRepository {

    // ── Reads ───────────────────────────────────────────────────────────

    override suspend fun sentencesForTrack(trackId: Long): List<Sentence> = withContext(ioDispatcher) {
        val offset = trackDao.findById(trackId)?.offsetMs ?: 0L
        sentenceDao.forTrack(trackId).map { it.toDomain(offset) }
    }

    /**
     * Combines the cue stream with the track's offset so that nudging the sync
     * re-emits corrected timings without the player having to resubscribe.
     */
    override fun observeSentencesForTrack(trackId: Long): Flow<List<Sentence>> = flow {
        val mediaItemId = trackDao.findById(trackId)?.mediaItemId
        val offsets = if (mediaItemId == null) {
            flowOf(0L)
        } else {
            trackDao.observeForMedia(mediaItemId)
                .map { tracks -> tracks.firstOrNull { it.id == trackId }?.offsetMs ?: 0L }
                .distinctUntilChanged()
        }
        emitAll(
            combine(sentenceDao.observeForTrack(trackId), offsets) { rows, offset ->
                rows.map { it.toDomain(offset) }
            }
        )
    }

    override suspend fun getSentence(id: Long): Sentence? = withContext(ioDispatcher) {
        val entity = sentenceDao.findById(id) ?: return@withContext null
        entity.toDomain(trackDao.findById(entity.subtitleTrackId)?.offsetMs ?: 0L)
    }

    override suspend fun translate(sentenceId: Long, targetLanguage: String): Result<String> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = requireNotNull(sentenceDao.findById(sentenceId)) {
                    "Unknown sentence $sentenceId"
                }
                // A cached translation is authoritative: translation is slow and
                // may require a model download.
                entity.translation?.takeIf { it.isNotBlank() }?.let { return@runCatching it }

                val translated = translator.translate(entity.text, targetLanguage).getOrThrow()
                sentenceDao.setTranslation(sentenceId, translated)
                translated
            }
        }

    // ── Favourites ──────────────────────────────────────────────────────

    override fun observeSaved(): Flow<List<SavedSentence>> =
        favoriteDao.observeAll(LOCAL_USER_ID).map { rows -> rows.map { it.toDomain() } }

    override fun observeSavedIds(): Flow<Set<Long>> =
        favoriteDao.observeFavoriteIds(LOCAL_USER_ID).map { it.toSet() }

    override fun observeSavedCount(): Flow<Int> = favoriteDao.observeCount(LOCAL_USER_ID)

    override suspend fun save(sentenceId: Long, note: String?) = withContext(ioDispatcher) {
        // The DAO ignores conflicts, so saving twice is a no-op rather than an error.
        favoriteDao.insert(
            FavoriteSentenceEntity(
                userId = LOCAL_USER_ID,
                sentenceId = sentenceId,
                note = note?.takeIf { it.isNotBlank() },
            )
        )
        Unit
    }

    override suspend fun unsave(sentenceId: Long) = withContext(ioDispatcher) {
        favoriteDao.delete(LOCAL_USER_ID, sentenceId)
    }

    override suspend fun toggleSaved(sentenceId: Long): Boolean = withContext(ioDispatcher) {
        val isSaved = favoriteDao.observeFavoriteIds(LOCAL_USER_ID).first().contains(sentenceId)
        if (isSaved) {
            favoriteDao.delete(LOCAL_USER_ID, sentenceId)
        } else {
            favoriteDao.insert(FavoriteSentenceEntity(userId = LOCAL_USER_ID, sentenceId = sentenceId))
        }
        !isSaved
    }

    override suspend fun setNote(favoriteId: Long, note: String?) = withContext(ioDispatcher) {
        favoriteDao.setNote(favoriteId, note?.takeIf { it.isNotBlank() })
    }

    // ── Search ──────────────────────────────────────────────────────────

    /**
     * FTS first, `LIKE` as the safety net.
     *
     * `MATCH` is orders of magnitude faster but rejects punctuation, cannot
     * express a mid-word substring, and throws on malformed expressions — all
     * of which are normal user input in a search box.
     */
    override suspend fun search(query: String, limit: Int): List<Sentence> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val ftsQuery = toFtsQuery(trimmed)
        val ftsHits = if (ftsQuery.isEmpty()) {
            emptyList()
        } else {
            runCatching { sentenceDao.searchFts(ftsQuery, limit) }
                .onFailure { LingoLog.w(TAG, "FTS query '$ftsQuery' failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        val rows = ftsHits.ifEmpty { sentenceDao.searchLike(trimmed, limit) }

        // Offsets are fetched once per track; a result set usually spans few tracks.
        val offsets = mutableMapOf<Long, Long>()
        rows.map { row ->
            val offset = offsets.getOrPut(row.subtitleTrackId) {
                trackDao.findById(row.subtitleTrackId)?.offsetMs ?: 0L
            }
            row.toDomain(offset)
        }
    }

    companion object {
        private const val TAG = "SentenceRepository"

        /** Characters that are operators in an FTS MATCH expression. */
        private val FTS_BREAKING = Regex("""["*^:()\-+~\[\]{}<>!?,.;/\\|&%$#@=]""")

        /**
         * Rewrites free text as a safe prefix query: operators are dropped and
         * the final token gets a `*` so results appear while the user types.
         */
        fun toFtsQuery(raw: String): String {
            val tokens = FTS_BREAKING.replace(raw, " ")
                .split(' ', '\t', '\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return ""
            return tokens.mapIndexed { index, token ->
                if (index == tokens.lastIndex) "$token*" else token
            }.joinToString(" ")
        }
    }
}
