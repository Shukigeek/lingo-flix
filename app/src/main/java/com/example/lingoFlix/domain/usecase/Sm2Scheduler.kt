package com.example.lingoFlix.domain.usecase

import com.example.lingoFlix.domain.model.LearningCard
import kotlin.math.roundToInt

/**
 * The SM-2 spaced-repetition algorithm.
 *
 * Kept free of Android and persistence types so the scheduling rules can be
 * verified on the JVM; the repository is only responsible for loading the
 * current state and storing the [Result].
 */
object Sm2Scheduler {

    /** The outcome of one review, ready to be written to `learning_progress`. */
    data class Result(
        val repetitions: Int,
        val intervalDays: Int,
        val easeFactor: Double,
        val nextReviewAt: Long,
        val lapses: Int,
        val masteryLevel: Int,
    )

    /** SM-2's lower bound; below this the card would be shown almost daily forever. */
    private const val MIN_EASE_FACTOR = 1.3

    /** Ease factor assigned to a card the first time it is reviewed. */
    private const val DEFAULT_EASE_FACTOR = 2.5

    /** Recall below this is treated as a failure and restarts the interval. */
    private const val PASSING_QUALITY = 3

    private const val DAY_MS = 24L * 60 * 60 * 1_000

    /**
     * Reschedules a card after one review.
     *
     * @param current the stored state, or `null` for a card seen for the first time.
     * @param quality SM-2 answer quality; values outside 0..5 are coerced rather
     *   than rejected so a UI bug can never crash a review session.
     * @param now injectable clock, which is what makes the result testable.
     */
    fun schedule(
        current: LearningCard?,
        quality: Int,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val q = quality.coerceIn(0, 5)
        val previousRepetitions = current?.repetitions ?: 0
        val previousInterval = current?.intervalDays ?: 0
        val previousEase = current?.easeFactor ?: DEFAULT_EASE_FACTOR
        val previousLapses = current?.lapses ?: 0

        // The ease factor always reacts to the answer, including on a lapse.
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        val easeFactor = (previousEase + delta).coerceAtLeast(MIN_EASE_FACTOR)

        val failed = q < PASSING_QUALITY
        val repetitions = if (failed) 0 else previousRepetitions + 1
        val intervalDays = when {
            failed -> 1
            repetitions == 1 -> 1
            repetitions == 2 -> 6
            else -> (previousInterval * easeFactor).roundToInt().coerceAtLeast(1)
        }

        return Result(
            repetitions = repetitions,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            nextReviewAt = now + intervalDays * DAY_MS,
            lapses = if (failed) previousLapses + 1 else previousLapses,
            masteryLevel = if (failed) 0 else repetitions.coerceIn(0, 5),
        )
    }
}
