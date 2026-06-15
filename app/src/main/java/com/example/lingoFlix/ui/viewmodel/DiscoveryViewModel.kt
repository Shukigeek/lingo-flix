package com.example.lingoFlix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lingoFlix.data.repository.TmdbRepository
import com.example.lingoFlix.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoveryViewModel(private val repository: TmdbRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Initial)
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    fun fetchTrending(apiKey: String) {
        if (apiKey.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = DiscoveryUiState.Loading
            val results = repository.getTrending(apiKey)
            if (results.isNotEmpty()) {
                _uiState.value = DiscoveryUiState.Success(results)
            } else {
                _uiState.value = DiscoveryUiState.Error("לא נמצאו סרטים פופולריים")
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
            _uiState.value = DiscoveryUiState.Loading
            val results = repository.search(query, apiKey)
            if (results.isNotEmpty()) {
                _uiState.value = DiscoveryUiState.Success(results)
            } else {
                _uiState.value = DiscoveryUiState.Error("לא נמצאו תוצאות ל-\"$query\"")
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

class DiscoveryViewModelFactory(private val repository: TmdbRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoveryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscoveryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
