package com.example.lingoFlix.data.mapper

import com.example.lingoFlix.data.local.db.dao.FavoriteSentenceDetails
import com.example.lingoFlix.data.local.db.dao.MediaItemWithDetails
import com.example.lingoFlix.data.local.db.entity.FolderKind
import com.example.lingoFlix.data.local.db.entity.LOCAL_USER_ID
import com.example.lingoFlix.data.local.db.entity.LearningProgressEntity
import com.example.lingoFlix.data.local.db.entity.LibraryFolderEntity
import com.example.lingoFlix.data.local.db.entity.MediaItemEntity
import com.example.lingoFlix.data.local.db.entity.MediaKind
import com.example.lingoFlix.data.local.db.entity.MediaMetadataEntity
import com.example.lingoFlix.data.local.db.entity.MediaSourceType
import com.example.lingoFlix.data.local.db.entity.SentenceEntity
import com.example.lingoFlix.data.local.db.entity.SubtitleTrackEntity
import com.example.lingoFlix.data.local.db.entity.WatchProgressEntity
import com.example.lingoFlix.domain.model.FolderKindType
import com.example.lingoFlix.domain.model.LearningCard
import com.example.lingoFlix.domain.model.LibraryFolder
import com.example.lingoFlix.domain.model.MediaItem
import com.example.lingoFlix.domain.model.MediaKindType
import com.example.lingoFlix.domain.model.MediaMetadata
import com.example.lingoFlix.domain.model.MediaSource
import com.example.lingoFlix.domain.model.SavedSentence
import com.example.lingoFlix.domain.model.Sentence
import com.example.lingoFlix.domain.model.SubtitleSourceType
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.domain.model.WatchProgress

/**
 * Entity to domain conversions.
 *
 * Kept in one place so the persistence vocabulary (string enums, nullable
 * relation lists, raw cue timings) never leaks past the data layer.
 */

// ── Library ─────────────────────────────────────────────────────────────

/** @param itemCount supplied by the caller because the DAO has no count-per-folder query. */
fun LibraryFolderEntity.toDomain(itemCount: Int = 0): LibraryFolder = LibraryFolder(
    id = id,
    parentId = parentId,
    name = name,
    kind = kind.toFolderKind(),
    itemCount = itemCount,
)

fun MediaMetadataEntity.toDomain(): MediaMetadata = MediaMetadata(
    tmdbId = tmdbId,
    imdbId = imdbId,
    year = year,
    imdbRating = imdbRating,
    voteCount = voteCount,
    genres = genres?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
)

fun WatchProgressEntity.toDomain(): WatchProgress = WatchProgress(
    mediaItemId = mediaItemId,
    positionMs = positionMs,
    completed = completed,
    updatedAt = updatedAt,
)

fun SubtitleTrackEntity.toDomain(): SubtitleTrack = SubtitleTrack(
    id = id,
    mediaItemId = mediaItemId,
    language = language,
    filePath = filePath,
    source = source.toSubtitleSource(),
    offsetMs = offsetMs,
    isPrimary = isPrimary,
    sentenceCount = sentenceCount,
)

/** Shallow conversion for the rare call site that has no relations loaded. */
fun MediaItemEntity.toDomain(): MediaItem = MediaItem(
    id = id,
    folderId = folderId,
    title = title,
    fileUri = fileUri,
    kind = kind.toMediaKind(),
    source = sourceType.toMediaSource(),
    seriesName = seriesName,
    season = season,
    episode = episode,
    durationMs = durationMs,
    thumbnailPath = thumbnailPath,
)

/**
 * @param userId selects which user's resume position is attached; the relation
 *   query returns every user's row.
 */
fun MediaItemWithDetails.toDomain(userId: String = LOCAL_USER_ID): MediaItem = item.toDomain().copy(
    metadata = metadata?.toDomain(),
    subtitleTracks = subtitleTracks.sortedByDescending { it.isPrimary }.map { it.toDomain() },
    watchProgress = progress.firstOrNull { it.userId == userId }?.toDomain(),
)

// ── Sentences ───────────────────────────────────────────────────────────

/**
 * @param offsetMs the owning track's sync correction. It is applied here so no
 *   caller can forget it; the result is clamped because a negative seek target
 *   is meaningless to the player.
 */
fun SentenceEntity.toDomain(offsetMs: Long = 0): Sentence = Sentence(
    id = id,
    subtitleTrackId = subtitleTrackId,
    index = indexInTrack,
    startMs = (startMs + offsetMs).coerceAtLeast(0),
    endMs = (endMs + offsetMs).coerceAtLeast(0),
    text = text,
    translation = translation,
)

fun FavoriteSentenceDetails.toDomain(): SavedSentence = SavedSentence(
    favoriteId = favoriteId,
    sentence = sentence.toDomain(trackOffsetMs),
    mediaItemId = mediaItemId,
    mediaTitle = mediaTitle,
    mediaUri = mediaUri,
    seriesName = seriesName,
    season = season,
    episode = episode,
    note = note,
    savedAt = savedAt,
    masteryLevel = masteryLevel ?: 0,
    nextReviewAt = nextReviewAt,
)

fun LearningProgressEntity.toDomain(): LearningCard = LearningCard(
    sentenceId = sentenceId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    lastReviewAt = lastReviewAt,
    nextReviewAt = nextReviewAt,
    lapses = lapses,
    masteryLevel = masteryLevel,
)

// ── String enums ────────────────────────────────────────────────────────
// Unknown values fall back to the safest variant rather than throwing: the
// column is plain TEXT and an older build may have written something else.

fun String?.toFolderKind(): FolderKindType = when (this) {
    FolderKind.MOVIES -> FolderKindType.MOVIES
    FolderKind.SERIES -> FolderKindType.SERIES
    FolderKind.SEASON -> FolderKindType.SEASON
    else -> FolderKindType.CUSTOM
}

fun String?.toMediaKind(): MediaKindType =
    if (this == MediaKind.EPISODE) MediaKindType.EPISODE else MediaKindType.MOVIE

fun String?.toMediaSource(): MediaSource = when (this) {
    MediaSourceType.PC_CLIENT -> MediaSource.PC_CLIENT
    MediaSourceType.URL -> MediaSource.URL
    else -> MediaSource.LOCAL
}

fun String?.toSubtitleSource(): SubtitleSourceType =
    SubtitleSourceType.entries.firstOrNull { it.name == this } ?: SubtitleSourceType.MANUAL
