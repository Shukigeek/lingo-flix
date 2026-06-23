package com.example.lingoFlix.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.model.UserProfile
import java.io.File

/**
 * Shared state for the application UI.
 */
class MainViewModel : ViewModel() {
    var navigationStack by mutableStateOf(listOf("dashboard"))
    var currentUser by mutableStateOf(UserProfile("main_user", "לומד", 0))
    
    var selectedVideoFile by mutableStateOf<File?>(null)
    var selectedVideoUri by mutableStateOf<Uri?>(null)
    var practiceClips by mutableStateOf<List<SubtitleClip>?>(null)
    
    var isQuizModeActive by mutableStateOf(false)
    var isRandomModeActive by mutableStateOf(false)
    var quizDifficulty by mutableStateOf("קל")
    var quizType by mutableStateOf("typing")

    fun navigateTo(screen: String) {
        if (navigationStack.last() != screen) {
            navigationStack = navigationStack + screen
        }
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }
}
