package com.example.lingoFlix.domain.repository

import com.example.lingoFlix.domain.model.LearningStats
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.model.SavedSentence
import com.example.lingoFlix.domain.model.Sentence
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.domain.model.SubtitleSourceType
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts. The UI layer depends on these interfaces only, which
 * keeps ViewModels testable with simple fakes.
 */

interface LibraryRepository {

    fun observeFolders(parentId: Long?): Flow<List<LibraryFolder>>

    fun observeItems(folderId: Long?): Flow<List<MediaItem>>

    fun observeAllItems(): Flow<List<MediaItem>>

    fun observeItem(id: Long): Flow<MediaItem?>

    fun observeContinueWatching(limit: Int = 10): Flow<List<MediaItem>>

    fun searchItems(query: String): Flow<List<MediaItem>>

    suspend fun getItem(id: Long): MediaItem?

    /**
     * Imports a video the user picked. Returns the new item's id, or the
     * existing id when the file is already in the library.
     */
    suspend fun importVideo(uri: String, folderId: Long? = null): Result<Long>

    /** Recursively imports every supported video under a picked tree URI. */
    suspend fun importFolder(treeUri: String, parentFolderId: Long? = null): Result<ImportSummary>

    suspend fun createFolder(parentId: Long?, name: String): Result<Long>

    suspend fun renameFolder(id: Long, name: String)

    suspend fun deleteFolder(id: Long)

    suspend fun renameItem(id: Long, title: String)

    suspend fun moveItems(ids: List<Long>, folderId: Long?)

    suspend fun deleteItem(id: Long)

    /** Drops library entries whose underlying file no longer exists. */
    suspend fun pruneMissingFiles(): Int

    suspend fun saveProgress(mediaItemId: Long, positionMs: Long, completed: Boolean = false)
}

data class ImportSummary(
    val imported: Int,
    val skipped: Int,
    val subtitlesMatched: Int,
    val failed: Int,
)

interface SubtitleRepository {

    fun observeTracks(mediaItemId: Long): Flow<List<SubtitleTrack>>

    /**
     * Attaches a subtitle file, parses it and persists its sentences.
     * Returns the new track id.
     */
    suspend fun attachSubtitle(
        mediaItemId: Long,
        uri: String,
        language: String? = null,
        source: SubtitleSourceType = SubtitleSourceType.MANUAL,
        makePrimary: Boolean = true,
    ): Result<Long>

    suspend fun deleteTrack(trackId: Long)

    suspend fun setPrimaryTrack(mediaItemId: Long, trackId: Long)

    /** Shifts every cue in the track by [offsetMs]; positive means later. */
    suspend fun setOffset(trackId: Long, offsetMs: Long)

    /** Re-parses the file on disk, e.g. after the user edited it externally. */
    suspend fun reparse(trackId: Long): Result<Int>
}

interface SentenceRepository {

    /** All sentences of a track, ordered by start time, with offset applied. */
    suspend fun sentencesForTrack(trackId: Long): List<Sentence>

    fun observeSentencesForTrack(trackId: Long): Flow<List<Sentence>>

    suspend fun getSentence(id: Long): Sentence?

    suspend fun translate(sentenceId: Long, targetLanguage: String): Result<String>

    // ── Favourites ──────────────────────────────────────────────────────

    fun observeSaved(): Flow<List<SavedSentence>>

    fun observeSavedIds(): Flow<Set<Long>>

    fun observeSavedCount(): Flow<Int>

    suspend fun save(sentenceId: Long, note: String? = null)

    suspend fun unsave(sentenceId: Long)

    suspend fun toggleSaved(sentenceId: Long): Boolean

    suspend fun setNote(favoriteId: Long, note: String?)

    /** Full-text search over every parsed sentence in the library. */
    suspend fun search(query: String, limit: Int = 100): List<Sentence>
}

interface PracticeRepository {

    /** Sentences whose SM-2 review date has passed, oldest first. */
    suspend fun dueQueue(limit: Int = 20): List<SavedSentence>

    fun observeDueCount(): Flow<Int>

    /**
     * Records the outcome of one review and reschedules the sentence.
     *
     * @param quality SM-2 answer quality, 0 (blackout) to 5 (perfect recall).
     */
    suspend fun recordAnswer(sentenceId: Long, quality: Int)

    fun observeStats(): Flow<LearningStats>
}
