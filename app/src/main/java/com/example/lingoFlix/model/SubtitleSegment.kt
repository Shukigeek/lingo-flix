package com.example.lingoFlix.model

data class SubtitleSegment(
    val index: Int,
    val startTime: Long, // in milliseconds
    val endTime: Long,   // in milliseconds
    val text: String
)
