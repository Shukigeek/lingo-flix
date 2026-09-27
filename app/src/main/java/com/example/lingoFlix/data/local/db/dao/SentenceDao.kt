package com.example.lingoFlix.data.local.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.lingoFlix.data.local.db.entity.FavoriteSentenceEntity
import com.example.lingoFlix.data.local.db.entity.LearningProgressEntity
import com.example.lingoFlix.data.local.db.entity.SentenceEntity
import com.example.lingoFlix.data.local.db.entity.SubtitleTrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * A saved sentence joined with everything the "My Sentences" screen shows:
 * where it came from and how well the user knows it.
 */
data class FavoriteSentenceDetails(
    @Embedded val sentence: SentenceEntity,
    val favoriteId: Long,
    val note: String?,
    val savedAt: Long,
    val mediaItemId: Long,
    val mediaTitle: String,
    val seriesName: String?,
    val season: Int?,
    val episode: Int?,
    val mediaUri: String,
    val trackOffsetMs: Long,
    val masteryLevel: Int?,
    val nextReviewAt: Long?,
)

@Dao
interface SubtitleTrackDao {

    @Query("SELECT * FROM subtitle_track WHERE mediaItemId = :mediaItemId ORDER BY isPrimary DESC")
    fun observeForMedia(mediaItemId: Long): Flow<List<SubtitleTrackEntity>>

    @Query("SELECT * FROM subtitle_track WHERE mediaItemId = :mediaItemId AND isPrimary = 1 LIMIT 1")
    suspend fun findPrimary(mediaItemId: Long): SubtitleTrackEntity?

    @Query("SELECT * FROM subtitle_track WHERE id = :id")
    suspend fun findById(id: Long): SubtitleTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: SubtitleTrackEntity): Long

    @Update
    suspend fun update(track: SubtitleTrackEntity)

    @Query("UPDATE subtitle_track SET offsetMs = :offsetMs WHERE id = :id")
    suspend fun setOffset(id: Long, offsetMs: Long)

    @Query("UPDATE subtitle_track SET isPrimary = 0 WHERE mediaItemId = :mediaItemId")
    suspend fun clearPrimaryFlag(mediaItemId: Long)

    @Query("UPDATE subtitle_track SET sentenceCount = :count WHERE id = :id")
    suspend fun setSentenceCount(id: Long, count: Int)

    @Query("DELETE FROM subtitle_track WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Makes [trackId] the single primary track for its media item. */
    @Transaction
    suspend fun makePrimary(mediaItemId: Long, trackId: Long) {
        clearPrimaryFlag(mediaItemId)
        findById(trackId)?.let { update(it.copy(isPrimary = true)) }
    }
}

@Dao
interface SentenceDao {

    @Query("SELECT * FROM sentence WHERE subtitleTrackId = :trackId ORDER BY indexInTrack")
    suspend fun forTrack(trackId: Long): List<SentenceEntity>

    @Query("SELECT * FROM sentence WHERE subtitleTrackId = :trackId ORDER BY indexInTrack")
    fun observeForTrack(trackId: Long): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentence WHERE id = :id")
    suspend fun findById(id: Long): SentenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sentences: List<SentenceEntity>): List<Long>

    @Query("UPDATE sentence SET translation = :translation WHERE id = :id")
    suspend fun setTranslation(id: Long, translation: String)

    @Query("DELETE FROM sentence WHERE subtitleTrackId = :trackId")
    suspend fun deleteForTrack(trackId: Long)

    /** Replaces a track's sentences atomically so playback never sees a half-parsed file. */
    @Transaction
    suspend fun replaceForTrack(trackId: Long, sentences: List<SentenceEntity>) {
        deleteForTrack(trackId)
        insertAll(sentences)
    }

    /** Full-text search across every parsed sentence. */
    @Query(
        """
        SELECT s.* FROM sentence s
        JOIN sentence_fts fts ON fts.rowid = s.id
        WHERE sentence_fts MATCH :query
        LIMIT :limit
        """
    )
    suspend fun searchFts(query: String, limit: Int = 100): List<SentenceEntity>

    /** Fallback substring search for queries FTS cannot express (single letters, partial words). */
    @Query("SELECT * FROM sentence WHERE text LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchLike(query: String, limit: Int = 100): List<SentenceEntity>
}

@Dao
interface FavoriteSentenceDao {

    @Query(
        """
        SELECT s.*,
               f.id           AS favoriteId,
               f.note         AS note,
               f.createdAt    AS savedAt,
               mi.id          AS mediaItemId,
               mi.title       AS mediaTitle,
               mi.seriesName  AS seriesName,
               mi.season      AS season,
               mi.episode     AS episode,
               mi.fileUri     AS mediaUri,
               st.offsetMs    AS trackOffsetMs,
               lp.masteryLevel AS masteryLevel,
               lp.nextReviewAt AS nextReviewAt
        FROM favorite_sentence f
        JOIN sentence s        ON s.id = f.sentenceId
        JOIN subtitle_track st ON st.id = s.subtitleTrackId
        JOIN media_item mi     ON mi.id = st.mediaItemId
        LEFT JOIN learning_progress lp
               ON lp.sentenceId = s.id AND lp.userId = f.userId
        WHERE f.userId = :userId AND f.syncState != 'PENDING_DELETE'
        ORDER BY f.createdAt DESC
        """
    )
    fun observeAll(userId: String): Flow<List<FavoriteSentenceDetails>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_sentence WHERE userId = :userId AND sentenceId = :sentenceId)")
    fun observeIsFavorite(userId: String, sentenceId: Long): Flow<Boolean>

    @Query("SELECT sentenceId FROM favorite_sentence WHERE userId = :userId")
    fun observeFavoriteIds(userId: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteSentenceEntity): Long

    @Query("DELETE FROM favorite_sentence WHERE userId = :userId AND sentenceId = :sentenceId")
    suspend fun delete(userId: String, sentenceId: Long)

    @Query("UPDATE favorite_sentence SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String?)

    @Query("SELECT COUNT(*) FROM favorite_sentence WHERE userId = :userId")
    fun observeCount(userId: String): Flow<Int>
}

@Dao
interface LearningProgressDao {

    @Query("SELECT * FROM learning_progress WHERE userId = :userId AND sentenceId = :sentenceId")
    suspend fun find(userId: String, sentenceId: Long): LearningProgressEntity?

    @Upsert
    suspend fun upsert(progress: LearningProgressEntity)

    /**
     * The spaced-repetition queue: saved sentences whose review date has passed,
     * oldest first. Backed by the index on `nextReviewAt`.
     */
    @Query(
        """
        SELECT s.*,
               f.id           AS favoriteId,
               f.note         AS note,
               f.createdAt    AS savedAt,
               mi.id          AS mediaItemId,
               mi.title       AS mediaTitle,
               mi.seriesName  AS seriesName,
               mi.season      AS season,
               mi.episode     AS episode,
               mi.fileUri     AS mediaUri,
               st.offsetMs    AS trackOffsetMs,
               lp.masteryLevel AS masteryLevel,
               lp.nextReviewAt AS nextReviewAt
        FROM favorite_sentence f
        JOIN sentence s        ON s.id = f.sentenceId
        JOIN subtitle_track st ON st.id = s.subtitleTrackId
        JOIN media_item mi     ON mi.id = st.mediaItemId
        LEFT JOIN learning_progress lp
               ON lp.sentenceId = s.id AND lp.userId = f.userId
        WHERE f.userId = :userId
          AND f.syncState != 'PENDING_DELETE'
          AND (lp.nextReviewAt IS NULL OR lp.nextReviewAt <= :now)
        ORDER BY lp.nextReviewAt IS NULL DESC, lp.nextReviewAt ASC
        LIMIT :limit
        """
    )
    suspend fun dueQueue(userId: String, now: Long, limit: Int = 20): List<FavoriteSentenceDetails>

    @Query(
        """
        SELECT COUNT(*) FROM favorite_sentence f
        LEFT JOIN learning_progress lp
               ON lp.sentenceId = f.sentenceId AND lp.userId = f.userId
        WHERE f.userId = :userId
          AND f.syncState != 'PENDING_DELETE'
          AND (lp.nextReviewAt IS NULL OR lp.nextReviewAt <= :now)
        """
    )
    fun observeDueCount(userId: String, now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM learning_progress WHERE userId = :userId AND masteryLevel >= 4")
    fun observeLearnedCount(userId: String): Flow<Int>
}
