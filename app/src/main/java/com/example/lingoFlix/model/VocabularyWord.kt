package com.example.lingoFlix.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary")
data class VocabularyWord(
    @PrimaryKey val word: String,
    val translation: String? = null,
    val sourceLanguage: String = "English",
    val lastReviewedMs: Long = System.currentTimeMillis(),
    val nextReviewMs: Long = System.currentTimeMillis(),
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val masteryLevel: Int = 0, // 0 to 5
    val wrongCount: Int = 0,
    val contextSentence: String? = null
)
