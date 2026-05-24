package com.example.lingoFlix.model

data class Question(
    val fullText: String,
    val maskedText: String, // e.g. "Hola ____ como estas?"
    val targetWords: List<String> // The words that were masked
)
