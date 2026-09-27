package com.example.lingoFlix.domain.usecase

import com.example.lingoFlix.domain.model.LearningCard
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the SM-2 rules. The scheduler decides when the user sees a sentence
 * again, so a silent change here degrades learning without any visible failure.
 */
class Sm2SchedulerTest {

    private val now = 1_700_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1_000

    private fun card(
        repetitions: Int,
        intervalDays: Int,
        easeFactor: Double = 2.5,
        lapses: Int = 0,
    ) = LearningCard(
        sentenceId = 1,
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        lastReviewAt = 0,
        nextReviewAt = 0,
        lapses = lapses,
        masteryLevel = repetitions.coerceIn(0, 5),
    )

    @Test
    fun `first successful review schedules one day ahead`() {
        val result = Sm2Scheduler.schedule(current = null, quality = 5, now = now)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(1, result.masteryLevel)
        assertEquals(0, result.lapses)
    }

    @Test
    fun `classic progression is one then six then fifteen days`() {
        val first = Sm2Scheduler.schedule(current = null, quality = 4, now = now)
        assertEquals(1, first.intervalDays)

        val second = Sm2Scheduler.schedule(
            card(first.repetitions, first.intervalDays, first.easeFactor),
            quality = 4,
            now = now,
        )
        assertEquals(6, second.intervalDays)

        val third = Sm2Scheduler.schedule(
            card(second.repetitions, second.intervalDays, second.easeFactor),
            quality = 4,
            now = now,
        )
        assertEquals(15, third.intervalDays)
    }

    @Test
    fun `quality four leaves the ease factor unchanged`() {
        val result = Sm2Scheduler.schedule(card(1, 1), quality = 4, now = now)

        assertEquals(2.5, result.easeFactor, 1e-9)
    }

    @Test
    fun `quality five raises the ease factor by one tenth`() {
        val result = Sm2Scheduler.schedule(card(1, 1), quality = 5, now = now)

        assertEquals(2.6, result.easeFactor, 1e-9)
    }

    @Test
    fun `quality three lowers the ease factor`() {
        val result = Sm2Scheduler.schedule(card(1, 1), quality = 3, now = now)

        assertEquals(2.36, result.easeFactor, 1e-9)
    }

    @Test
    fun `ease factor never drops below the floor`() {
        val first = Sm2Scheduler.schedule(card(3, 15), quality = 0, now = now)
        assertEquals(1.7, first.easeFactor, 1e-9)

        val second = Sm2Scheduler.schedule(
            card(first.repetitions, first.intervalDays, first.easeFactor, first.lapses),
            quality = 0,
            now = now,
        )
        assertEquals(1.3, second.easeFactor, 1e-9)

        val third = Sm2Scheduler.schedule(
            card(second.repetitions, second.intervalDays, second.easeFactor, second.lapses),
            quality = 0,
            now = now,
        )
        assertEquals(1.3, third.easeFactor, 1e-9)
    }

    @Test
    fun `a failed review resets repetitions and the interval`() {
        val result = Sm2Scheduler.schedule(card(4, 30), quality = 2, now = now)

        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(0, result.masteryLevel)
    }

    @Test
    fun `a failed review increments lapses`() {
        val result = Sm2Scheduler.schedule(card(4, 30, lapses = 2), quality = 1, now = now)

        assertEquals(3, result.lapses)
    }

    @Test
    fun `a successful review preserves lapses`() {
        val result = Sm2Scheduler.schedule(card(2, 6, lapses = 2), quality = 5, now = now)

        assertEquals(2, result.lapses)
    }

    @Test
    fun `the third interval multiplies the previous one by the updated ease factor`() {
        val result = Sm2Scheduler.schedule(card(2, 6, easeFactor = 2.5), quality = 5, now = now)

        assertEquals(2.6, result.easeFactor, 1e-9)
        assertEquals(16, result.intervalDays)
    }

    @Test
    fun `quality above five is coerced`() {
        val coerced = Sm2Scheduler.schedule(card(1, 1), quality = 42, now = now)
        val explicit = Sm2Scheduler.schedule(card(1, 1), quality = 5, now = now)

        assertEquals(explicit, coerced)
    }

    @Test
    fun `quality below zero is coerced`() {
        val coerced = Sm2Scheduler.schedule(card(1, 1), quality = -7, now = now)
        val explicit = Sm2Scheduler.schedule(card(1, 1), quality = 0, now = now)

        assertEquals(explicit, coerced)
    }

    @Test
    fun `next review is the interval in whole days after now`() {
        val result = Sm2Scheduler.schedule(card(2, 6), quality = 4, now = now)

        assertEquals(15, result.intervalDays)
        assertEquals(now + 15 * dayMs, result.nextReviewAt)
    }

    @Test
    fun `mastery level is capped at five`() {
        val result = Sm2Scheduler.schedule(card(7, 200), quality = 5, now = now)

        assertEquals(8, result.repetitions)
        assertEquals(5, result.masteryLevel)
    }

    @Test
    fun `an interval of zero still advances at least one day`() {
        val result = Sm2Scheduler.schedule(card(5, 0), quality = 5, now = now)

        assertEquals(1, result.intervalDays)
    }

    @Test
    fun `recovery after a lapse restarts at one day`() {
        val lapsed = Sm2Scheduler.schedule(card(4, 30), quality = 0, now = now)
        val recovered = Sm2Scheduler.schedule(
            card(lapsed.repetitions, lapsed.intervalDays, lapsed.easeFactor, lapsed.lapses),
            quality = 5,
            now = now,
        )

        assertEquals(1, recovered.repetitions)
        assertEquals(1, recovered.intervalDays)
        assertEquals(1, recovered.lapses)
    }
}
