package com.example.lingoFlix.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_metadata")
data class VideoMetadata(
    @PrimaryKey val filePath: String,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val description: String? = null,
    val thumbnailPath: String? = null,
    val lastWatchedMs: Long = 0,
    val isFavorite: Boolean = false,
    val isLinked: Boolean = false, // Added to track random pool
    val tags: String? = null // Comma separated tags
)
