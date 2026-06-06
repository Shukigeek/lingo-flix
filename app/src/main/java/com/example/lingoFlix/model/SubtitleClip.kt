package com.example.lingoFlix.model

import android.net.Uri

data class SubtitleClip(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val videoUri: Uri
)
