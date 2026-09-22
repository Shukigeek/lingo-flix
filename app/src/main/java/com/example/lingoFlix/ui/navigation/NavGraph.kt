package com.example.lingoFlix.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.data.remote.SkillBuilderEngine
import com.example.lingoFlix.data.repository.SkillRepository
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.model.UserProfile
import com.example.lingoFlix.ui.*
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Main navigation logic extracted to reduce MainActivity size.
 * Handles screen switching and shared state.
 */
@Composable
fun NavGraph(
    activity: ComponentActivity,
    database: AppDatabase,
    navigationStack: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onUpdateNavigationStack: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentScreen = navigationStack.lastOrNull() ?: "dashboard"

    // Shared State (Usually would be in a ViewModel, but keeping local for now to maintain consistency)
    var currentUser by remember { mutableStateOf<UserProfile?>(UserProfile("main_user", "לומד", 0)) }
    var selectedVideoFile by remember { mutableStateOf<File?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var practiceClips by remember { mutableStateOf<List<SubtitleClip>?>(null) }
    var isQuizModeActive by remember { mutableStateOf(false) }
    var isRandomModeActive by remember { mutableStateOf(false) }
    var quizDifficulty by remember { mutableStateOf("קל") }
    var quizType by remember { mutableStateOf("typing") }

    val skillDao = remember { database.skillDao() }
    val dynamicSkills by skillDao.getActiveSkills().collectAsState(initial = emptyList())

    when (currentScreen) {
        "admin_dashboard" -> AdminDashboardScreen(
            onBack = onNavigateBack,
            configDao = database.configDao(),
            recommendedMediaDao = database.recommendedMediaDao()
        )
        "discovery" -> DiscoveryScreen(userId = currentUser?.id ?: "guest")
        "settings" -> SettingsScreen(
            currentGeminiApiKey = try { SecurityUtils.getUserApiKey(context, currentUser?.id ?: "guest") ?: "" } catch (e: Exception) { "" },
            currentTmdbApiKey = try { SecurityUtils.getTmdbApiKey(context, currentUser?.id ?: "guest") ?: "" } catch (e: Exception) { "" },
            currentAnthropicApiKey = try { SecurityUtils.getAnthropicApiKey(context, currentUser?.id ?: "guest") ?: "" } catch (e: Exception) { "" },
            onSaveKeys = { gemini, tmdb, anthropic ->
                try {
                    SecurityUtils.saveUserApiKey(context, currentUser?.id ?: "guest", gemini)
                    SecurityUtils.saveTmdbApiKey(context, currentUser?.id ?: "guest", tmdb)
                    SecurityUtils.saveAnthropicApiKey(context, currentUser?.id ?: "guest", anthropic)
                    LingoLog.i("NavGraph", "API Keys saved successfully")
                } catch (e: Exception) {
                    LingoLog.e("NavGraph", "Error saving API keys", e)
                }
            },
            onBack = onNavigateBack
        )
        "profile" -> ProfileScreen(
            userName = currentUser?.name ?: "לומד",
            onNameChange = { newName -> currentUser = currentUser?.copy(name = newName) },
            totalXP = 0, // Should come from statsManager
            currentStreak = 0, // Should come from statsManager
            onBack = onNavigateBack
        )
        // ... Other screens would be mapped here
    }
}
