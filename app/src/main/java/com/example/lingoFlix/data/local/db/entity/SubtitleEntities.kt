package com.example.lingoFlix.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One subtitle file attached to a media item. An item may have several tracks
 * (for example the target language plus the learner's native language).
 */
@Entity(
    tableName = "subtitle_track",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("mediaItemId")],
)
data class SubtitleTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaItemId: Long,
    /** ISO 639-1, e.g. `"en"`. */
    val language: String,
    val filePath: String,
    /** One of [SubtitleSource]. */
    val source: String = SubtitleSource.MANUAL,
    /**
     * Manual sync correction in milliseconds, applied on top of the parsed
     * timings. Positive shifts subtitles later (project book FR-206).
     */
    val offsetMs: Long = 0,
    /** The track used for learning; exactly one per media item. */
    val isPrimary: Boolean = true,
    val sentenceCount: Int = 0,
    val importedAt: Long = System.currentTimeMillis(),
)

object SubtitleSource {
    const val MANUAL = "MANUAL"
    const val AUTO_MATCH = "AUTO_MATCH"
    const val EMBEDDED = "EMBEDDED"
    const val DOWNLOADED = "DOWNLOADED"
    const val AI_GENERATED = "AI_GENERATED"
}

/**
 * The atomic unit of learning: one subtitle block.
 *
 * Persisting sentences (rather than re-parsing the SRT on every open) is what
 * makes search, favourites and spaced repetition possible at all.
 */
@Entity(
    tableName = "sentence",
    foreignKeys = [
        ForeignKey(
            entity = SubtitleTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["subtitleTrackId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["subtitleTrackId", "startMs"]),
        Index("subtitleTrackId"),
    ],
)
data class SentenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subtitleTrackId: Long,
    /** Zero-based position within the track, used for next/previous navigation. */
    val indexInTrack: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    /** Filled lazily the first time the user asks to see a translation. */
    val translation: String? = null,
    val wordCount: Int = 0,
)

/**
 * Full-text search index over [SentenceEntity.text], backing the global search
 * screen (project book FR-405).
 */
@Fts4(contentEntity = SentenceEntity::class)
@Entity(tableName = "sentence_fts")
data class SentenceFtsEntity(
    val text: String,
)
