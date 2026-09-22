package com.example.lingoFlix.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_clips")
data class FavoriteClip(
    @PrimaryKey val id: String, // format: "startTime_textHash"
    val videoPath: String,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)
