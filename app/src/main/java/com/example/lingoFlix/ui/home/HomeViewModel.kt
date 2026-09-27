package com.example.lingoFlix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lingoFlix.domain.repository.LibraryRepository
import com.example.lingoFlix.domain.repository.PracticeRepository
import com.example.lingoFlix.domain.repository.SentenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the home screen.
 *
 * Holds no navigation knowledge on purpose: it exposes data, the NavHost owns
 * where a tap leads. Everything is derived from repository flows rather than
 * one-shot loads so the screen self-heals after an import or a practice session
 * without any manual refresh plumbing.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    libraryRepository: LibraryRepository,
    sentenceRepository: SentenceRepository,
    practiceRepository: PracticeRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        libraryRepository.observeContinueWatching(limit = CONTINUE_WATCHING_LIMIT),
        libraryRepository.observeAllItems().map { it.take(LIBRARY_ROW_LIMIT) },
        sentenceRepository.observeSavedCount(),
        practiceRepository.observeDueCount(),
        practiceRepository.observeStats().map { it.streakDays },
    ) { continueWatching, library, savedCount, dueCount, streakDays ->
        HomeUiState(
            continueWatching = continueWatching,
            recentLibrary = library,
            savedSentenceCount = savedCount,
            dueCount = dueCount,
            streakDays = streakDays,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        // Survives configuration changes and short backgrounding without
        // re-querying Room, but still lets the flows go cold when the user
        // genuinely leaves the screen.
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CONTINUE_WATCHING_LIMIT = 10
        const val LIBRARY_ROW_LIMIT = 20
    }
}
