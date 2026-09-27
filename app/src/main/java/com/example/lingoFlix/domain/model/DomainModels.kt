package com.example.lingoFlix.domain.model

/**
 * Pure domain models. No Room annotations, no Android types, no network DTOs —
 * the UI and business logic depend only on these.
 */

data class LibraryFolder(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val kind: FolderKindType,
    val itemCount: Int = 0,
)

enum class FolderKindType { MOVIES, SERIES, SEASON, CUSTOM }

enum class MediaKindType { MOVIE, EPISODE }

enum class MediaSource { LOCAL, PC_CLIENT, URL }

data class MediaItem(
    val id: Long,
    val folderId: Long?,
    val title: String,
    val fileUri: String,
    val kind: MediaKindType,
    val source: MediaSource,
    val seriesName: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val durationMs: Long = 0,
    val thumbnailPath: String? = null,
    val metadata: MediaMetadata? = null,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val watchProgress: WatchProgress? = null,
) {
    val hasSubtitles: Boolean get() = subtitleTracks.isNotEmpty()

    val primaryTrack: SubtitleTrack? get() = subtitleTracks.firstOrNull { it.isPrimary }

    /** `"Friends S02E04"` for episodes, plain title for films. */
    val displayTitle: String
        get() = when {
            kind == MediaKindType.EPISODE && seriesName != null && season != null && episode != null ->
                "$seriesName S%02dE%02d".format(season, episode)
            else -> title
        }

    /** 0f..1f watched fraction, used for progress bars. */
    val progressFraction: Float
        get() {
            val position = watchProgress?.positionMs ?: return 0f
            if (durationMs <= 0) return 0f
            return (position.toFloat() / durationMs).coerceIn(0f, 1f)
        }
}

data class MediaMetadata(
    val tmdbId: Int? = null,
    val imdbId: String? = null,
    val year: Int? = null,
    val imdbRating: Double? = null,
    val voteCount: Int? = null,
    val genres: List<String> = emptyList(),
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
)

enum class SubtitleSourceType { MANUAL, AUTO_MATCH, EMBEDDED, DOWNLOADED, AI_GENERATED }

data class SubtitleTrack(
    val id: Long,
    val mediaItemId: Long,
    val language: String,
    val filePath: String,
    val source: SubtitleSourceType,
    val offsetMs: Long = 0,
    val isPrimary: Boolean = true,
    val sentenceCount: Int = 0,
)

/**
 * The atomic unit of learning.
 *
 * [startMs] and [endMs] already include the track's sync offset, so callers can
 * seek to them directly.
 */
data class Sentence(
    val id: Long,
    val subtitleTrackId: Long,
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val translation: String? = null,
) {
    val durationMs: Long get() = endMs - startMs
}

data class WatchProgress(
    val mediaItemId: Long,
    val positionMs: Long,
    val completed: Boolean,
    val updatedAt: Long,
)

/**
 * A saved sentence enriched with its origin and learning state — exactly what
 * the "My Sentences" and practice screens need.
 */
data class SavedSentence(
    val favoriteId: Long,
    val sentence: Sentence,
    val mediaItemId: Long,
    val mediaTitle: String,
    val mediaUri: String,
    val seriesName: String?,
    val season: Int?,
    val episode: Int?,
    val note: String?,
    val savedAt: Long,
    val masteryLevel: Int,
    val nextReviewAt: Long?,
) {
    val sourceLabel: String
        get() = when {
            seriesName != null && season != null && episode != null ->
                "$seriesName S%02dE%02d".format(season, episode)
            else -> mediaTitle
        }

    val isDue: Boolean
        get() = nextReviewAt == null || nextReviewAt <= System.currentTimeMillis()
}

/** SM-2 scheduling state for one sentence. */
data class LearningCard(
    val sentenceId: Long,
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val lastReviewAt: Long,
    val nextReviewAt: Long,
    val lapses: Int,
    val masteryLevel: Int,
)

/** Aggregated numbers for the profile and home screens. */
data class LearningStats(
    val mediaCount: Int = 0,
    val completedCount: Int = 0,
    val savedSentenceCount: Int = 0,
    val learnedSentenceCount: Int = 0,
    val dueCount: Int = 0,
    val streakDays: Int = 0,
    val totalXp: Int = 0,
)
