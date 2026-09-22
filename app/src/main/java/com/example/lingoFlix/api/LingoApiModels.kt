package com.example.lingoFlix.api

data class TranslationRequest(
    val word: String,
    val sentence: String
)

data class TranslationResponse(
    val translation: String,
    val explanation: String,
    val alternative: String? = null
)

data class ActivityUpdate(
    val word: String,
    val is_correct: Boolean
)

data class WordStat(
    val word: String,
    val correct_count: Int,
    val wrong_count: Int,
    val last_practiced: String
)

data class CloudMovie(
    val id: String,
    val title: String,
    val description: String,
    val image_url: String,
    val category: String,
    val difficulty: String
)
