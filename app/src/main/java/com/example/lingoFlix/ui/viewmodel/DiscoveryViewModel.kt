package com.example.lingoFlix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lingoFlix.data.repository.TmdbRepository
import com.example.lingoFlix.data.repository.RecommendedMediaRepository
import com.example.lingoFlix.model.SearchResult
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiscoveryViewModel(
    private val tmdbRepository: TmdbRepository,
    private val recommendedRepository: RecommendedMediaRepository
) : ViewModel() {

    private val className = "DiscoveryViewModel"

    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Initial)
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    val developerRecommendations: StateFlow<List<SearchResult>> = 
        recommendedRepository.getRecommendedMediaFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        LingoLog.d(className, "DiscoveryViewModel initialized")
        viewModelScope.launch {
            recommendedRepository.seedDeveloperPicks()
        }
    }

    fun addToSystemRecommendations(item: SearchResult) {
        viewModelScope.launch {
            LingoLog.i(className, "Admin adding item to recommendations: ${item.title}")
            recommendedRepository.addRecommendation(item)
        }
    }

    fun fetchTrending(apiKey: String) {
        if (apiKey.isBlank()) return
        
        viewModelScope.launch {
            LingoLog.d(className, "Fetching trending content")
            _uiState.value = DiscoveryUiState.Loading
            try {
                val results = tmdbRepository.getTrending(apiKey)
                if (results.isNotEmpty()) {
                    _uiState.value = DiscoveryUiState.Success(results)
                } else {
                    _uiState.value = DiscoveryUiState.Error("לא נמצאו סרטים פופולריים")
                }
            } catch (e: Exception) {
                LingoLog.e(className, "Error fetching trending", e)
                _uiState.value = DiscoveryUiState.Error("שגיאה בתקשורת עם השרת")
            }
        }
    }

    fun search(query: String, apiKey: String) {
        if (query.isBlank()) {
            fetchTrending(apiKey)
            return
        }
        
        if (apiKey.isBlank()) {
            _uiState.value = DiscoveryUiState.Error("מפתח TMDB לא הוגדר בהגדרות")
            return
        }

        viewModelScope.launch {
            LingoLog.d(className, "Searching for: $query")
            _uiState.value = DiscoveryUiState.Loading
            try {
                val results = tmdbRepository.search(query, apiKey)
                if (results.isNotEmpty()) {
                    _uiState.value = DiscoveryUiState.Success(results)
                } else {
                    _uiState.value = DiscoveryUiState.Error("לא נמצאו תוצאות ל-\"$query\"")
                }
            } catch (e: Exception) {
                LingoLog.e(className, "Error during search", e)
                _uiState.value = DiscoveryUiState.Error("שגיאה בחיפוש")
            }
        }
    }
}

sealed class DiscoveryUiState {
    object Initial : DiscoveryUiState()
    object Loading : DiscoveryUiState()
    data class Success(val results: List<SearchResult>) : DiscoveryUiState()
    data class Error(val message: String) : DiscoveryUiState()
}

class DiscoveryViewModelFactory(
    private val tmdbRepository: TmdbRepository,
    private val recommendedRepository: RecommendedMediaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoveryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscoveryViewModel(tmdbRepository, recommendedRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
