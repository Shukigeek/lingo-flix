package com.example.lingoFlix.model

/**
 * Represent a single subtitle entry from an SRT file.
 */
data class SubtitleItem(
    val index: Int,
    val startTime: Long, // Start time in milliseconds
    val endTime: Long,   // End time in milliseconds
    val text: String     // The actual text/sentence
)

// NOTE: a second, duplicate `SrtParser` used to live in this file.
// Subtitle parsing now has exactly one implementation:
// com.example.lingoFlix.data.subtitle.SubtitleParser
