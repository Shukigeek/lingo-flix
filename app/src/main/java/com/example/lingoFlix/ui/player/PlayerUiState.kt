package com.example.lingoFlix.ui.player

import androidx.compose.runtime.Immutable
import com.example.lingoFlix.domain.model.Sentence

@Immutable
data class PlayerUiState(
    val mediaId: Long = 0,
    val title: String = "",
    val videoUri: String = "",
    val sentences: List<Sentence> = emptyList(),
    val currentIndex: Int = -1,
    val currentSentence: Sentence? = null,
    val isLooping: Boolean = false,
    val isPlaying: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isFavorite: Boolean = false,
    val isTranslationVisible: Boolean = true,
    val translationText: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@Immutable
data class PlayerActions(
    val onBack: () -> Unit,
    val onPlayPauseToggle: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onSeekToSentence: (Int) -> Unit,
    val onNextSentence: () -> Unit,
    val onPrevSentence: () -> Unit,
    val onToggleLoop: () -> Unit,
    val onToggleTranslation: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onSpeedChange: (Float) -> Unit,
    val onSaveProgress: (Long, Boolean) -> Unit,
)
