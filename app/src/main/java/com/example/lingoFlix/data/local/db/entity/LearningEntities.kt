package com.example.lingoFlix.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A sentence the user explicitly saved (project book §4).
 *
 * [syncState] lets the local database act as the source of truth while a
 * background job reconciles with the server.
 */
@Entity(
    tableName = "favorite_sentence",
    foreignKeys = [
        ForeignKey(
            entity = SentenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["userId", "sentenceId"], unique = true),
        Index("sentenceId"),
        Index("createdAt"),
    ],
)
data class FavoriteSentenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** `"local"` while the user is in guest mode. */
    val userId: String = LOCAL_USER_ID,
    val sentenceId: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** One of [SyncState]. */
    val syncState: String = SyncState.LOCAL,
)

const val LOCAL_USER_ID = "local"

object SyncState {
    const val LOCAL = "LOCAL"
    const val SYNCED = "SYNCED"
    const val PENDING_DELETE = "PENDING_DELETE"
}

/**
 * Resume position for a media item, updated periodically during playback.
 */
@Entity(
    tableName = "watch_progress",
    primaryKeys = ["userId", "mediaItemId"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("mediaItemId"), Index("updatedAt")],
)
data class WatchProgressEntity(
    val userId: String = LOCAL_USER_ID,
    val mediaItemId: Long,
    val positionMs: Long = 0,
    val completed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * SM-2 scheduling state for a single saved sentence (project book §14.2).
 *
 * [nextReviewAt] is indexed because the daily review queue is a range scan
 * over this column and runs on every app launch.
 */
@Entity(
    tableName = "learning_progress",
    primaryKeys = ["userId", "sentenceId"],
    foreignKeys = [
        ForeignKey(
            entity = SentenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sentenceId"), Index("nextReviewAt")],
)
data class LearningProgressEntity(
    val userId: String = LOCAL_USER_ID,
    val sentenceId: Long,
    /** Consecutive successful reviews; reset to 0 on a lapse. */
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val lastReviewAt: Long = 0,
    val nextReviewAt: Long = System.currentTimeMillis(),
    /** How many times the user forgot this sentence after having learned it. */
    val lapses: Int = 0,
    /** Derived 0..5 value used for the UI progress bar. */
    val masteryLevel: Int = 0,
)
