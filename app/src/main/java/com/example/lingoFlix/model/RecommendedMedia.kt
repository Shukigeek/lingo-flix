package com.example.lingoFlix.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing media recommended by the main developer.
 * Stores TMDB ID for fetching dynamic content and custom links for Telegram/actions.
 */
@Entity(tableName = "recommended_media")
data class RecommendedMedia(
    @PrimaryKey val tmdbId: Int,
    val title: String,
    val mediaType: String, // "movie" or "tv"
    val customTelegramLink: String? = null,
    val customYoutubeTrailerId: String? = null,
    val isDeveloperPick: Boolean = true,
    // Cached data from TMDB (updated periodically)
    val cachedDescription: String? = null,
    val cachedRating: Double = 0.0,
    val cachedImageUrl: String? = null
)
