package com.example.lingoFlix.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.lingoFlix.core.media.SentenceSynchronizer
import com.example.lingoFlix.domain.model.Sentence
import com.example.lingoFlix.domain.repository.LibraryRepository
import com.example.lingoFlix.domain.repository.SentenceRepository
import com.example.lingoFlix.domain.repository.SubtitleRepository
import com.example.lingoFlix.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val subtitleRepository: SubtitleRepository,
    private val sentenceRepository: SentenceRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Player>()
    private val mediaId = route.mediaId
    private val initialStartMs = route.startMs

    private val _uiState = MutableStateFlow(PlayerUiState(mediaId = mediaId))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadMediaAndSubtitles()
    }

    private fun loadMediaAndSubtitles() {
        viewModelScope.launch {
            try {
                val mediaItem = libraryRepository.getItem(mediaId)
                if (mediaItem == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Media not found") }
                    return@launch
                }

                val primaryTrack = mediaItem.primaryTrack
                val sentences = if (primaryTrack != null) {
                    sentenceRepository.sentencesForTrack(primaryTrack.id)
                } else {
                    emptyList()
                }

                val startPos = if (initialStartMs >= 0) {
                    initialStartMs
                } else {
                    mediaItem.watchProgress?.positionMs ?: 0L
                }

                val initialIndex = SentenceSynchronizer.indexAt(sentences, startPos)
                val initialSentence = if (initialIndex >= 0) sentences[initialIndex] else null

                _uiState.update {
                    it.copy(
                        title = mediaItem.displayTitle,
                        videoUri = mediaItem.fileUri,
                        sentences = sentences,
                        currentIndex = initialIndex,
                        currentSentence = initialSentence,
                        positionMs = startPos,
                        durationMs = mediaItem.durationMs,
                        isLoading = false,
                    )
                }

                // Observe favorite state for the initial sentence if any
                if (initialSentence != null) {
                    observeFavorite(initialSentence.id)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Error loading media") }
            }
        }
    }

    private fun observeFavorite(sentenceId: Long) {
        viewModelScope.launch {
            sentenceRepository.observeSavedIds().collect { savedIds ->
                _uiState.update { it.copy(isFavorite = savedIds.contains(sentenceId)) }
            }
        }
    }

    fun onPositionChanged(positionMs: Long) {
        _uiState.update { state ->
            val sentences = state.sentences
            val newIndex = SentenceSynchronizer.indexAt(sentences, positionMs)
            val newSentence = if (newIndex >= 0) sentences[newIndex] else null
            
            // Handle looping
            var adjustedPosition = positionMs
            if (state.isLooping && newSentence != null) {
                if (positionMs >= newSentence.endMs) {
                    adjustedPosition = newSentence.startMs
                }
            }

            state.copy(
                positionMs = adjustedPosition,
                currentIndex = newIndex,
                currentSentence = newSentence,
            )
        }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun toggleTranslation() {
        _uiState.update { it.copy(isTranslationVisible = !it.isTranslationVisible) }
    }

    fun toggleFavorite() {
        val sentence = _uiState.value.currentSentence ?: return
        viewModelScope.launch {
            sentenceRepository.toggleSaved(sentence.id)
        }
    }

    fun seekToSentence(index: Int) {
        val sentences = _uiState.value.sentences
        if (index in sentences.indices) {
            val sentence = sentences[index]
            _uiState.update {
                it.copy(
                    currentIndex = index,
                    currentSentence = sentence,
                    positionMs = sentence.startMs,
                )
            }
        }
    }

    fun nextSentence() {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx in state.sentences.indices) {
            seekToSentence(nextIdx)
        }
    }

    fun prevSentence() {
        val state = _uiState.value
        val prevIdx = (state.currentIndex - 1).coerceAtLeast(0)
        if (prevIdx in state.sentences.indices) {
            seekToSentence(prevIdx)
        }
    }

    fun saveProgress(positionMs: Long, completed: Boolean) {
        viewModelScope.launch {
            libraryRepository.saveProgress(mediaId, positionMs, completed)
        }
    }
}
