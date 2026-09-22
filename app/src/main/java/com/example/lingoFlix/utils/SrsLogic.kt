package com.example.lingoFlix.utils

import com.example.lingoFlix.model.VocabularyWord

object SrsLogic {
    /**
     * SM-2 Algorithm implementation
     * quality: 0-5 (0 = total blackout, 5 = perfect response)
     */
    fun calculateNextReview(word: VocabularyWord, quality: Int): VocabularyWord {
        val now = System.currentTimeMillis()
        var newInterval: Int
        var newEaseFactor = word.easeFactor

        if (quality >= 3) {
            // Correct response
            if (word.intervalDays == 0) {
                newInterval = 1
            } else if (word.intervalDays == 1) {
                newInterval = 6
            } else {
                newInterval = (word.intervalDays * word.easeFactor).toInt()
            }
            
            // Adjust ease factor
            newEaseFactor += (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
            if (newEaseFactor < 1.3f) newEaseFactor = 1.3f
        } else {
            // Incorrect response
            newInterval = 1
        }

        val nextReviewMs = now + (newInterval * 24L * 60L * 60L * 1000L)

        return word.copy(
            lastReviewedMs = now,
            nextReviewMs = nextReviewMs,
            intervalDays = newInterval,
            easeFactor = newEaseFactor,
            masteryLevel = if (quality >= 3) (word.masteryLevel + 1).coerceAtMost(5) else (word.masteryLevel - 1).coerceAtLeast(0),
            wrongCount = if (quality < 3) word.wrongCount + 1 else word.wrongCount
        )
    }
}
