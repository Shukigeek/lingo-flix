package com.example.lingoFlix.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.ui.navigation.BottomNavigationBar
import com.example.lingoFlix.ui.viewmodel.MainViewModel
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SecurityUtils
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.model.FavoriteClip
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MainContent(
    activity: ComponentActivity, 
    database: AppDatabase,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val navigationStack = mainViewModel.navigationStack
    val currentScreen = navigationStack.lastOrNull() ?: "dashboard"

    LaunchedEffect(Unit) {
        val statsManager = com.example.lingoFlix.data.UserStatsManager(context)
        statsManager.markActivityToday()
        mainViewModel.currentUser = mainViewModel.currentUser.copy(
            totalXP = statsManager.getXP()
        )
    }

    LingoLog.d("MainContent", "Rendering screen: $currentScreen")

    BackHandler(enabled = navigationStack.size > 1) {
        try {
            mainViewModel.navigateBack()
        } catch (e: Exception) {
            LingoLog.e("MainContent", "Error navigating back", e)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen in listOf("dashboard", "discovery", "video_list")) {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { target -> mainViewModel.navigationStack = listOf(target) }
                )
            }
        },
        containerColor = when(mainViewModel.backgroundResId) {
            1 -> Color(0xFFE3F2FD) // Light Blue
            2 -> Color(0xFFE8F5E9) // Light Green
            else -> Color.White
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "dashboard" -> DashboardRouter(mainViewModel, database)
                "discovery" -> DiscoveryScreen(mainViewModel.currentUser.id)
                "video_list" -> VideoListRouter(mainViewModel, database)
                "video_player" -> VideoPlayerRouter(mainViewModel, database)
                "battle" -> BattleRouter(mainViewModel, database)
                "word_deck" -> WordDeckScreen(database, onBack = { mainViewModel.navigateBack() })
                "settings" -> SettingsRouter(mainViewModel)
                "profile" -> ProfileScreen(
                    userName = mainViewModel.currentUser.name,
                    onNameChange = { mainViewModel.updateUserName(it) },
                    totalXP = mainViewModel.currentUser.totalXP,
                    currentStreak = 0, // TODO: Get from stats
                    onBack = { mainViewModel.navigateBack() }
                )
                "admin_dashboard" -> AdminDashboardScreen(
                    onBack = { mainViewModel.navigateBack() },
                    configDao = database.configDao(),
                    recommendedMediaDao = database.recommendedMediaDao()
                )
            }
        }
    }
}

@Composable
fun DashboardRouter(vm: MainViewModel, db: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Isolated router for Dashboard
    DashboardScreen(
        onMyVideos = { vm.navigateTo("video_list") },
        onAdminClick = { vm.navigateTo("admin_dashboard") },
        onProfileClick = { vm.navigateTo("profile") },
        onBattleMode = { vm.navigateTo("battle") },
        onFavorites = {
            // ... keep existing favorites logic ...
            scope.launch {
                val favorites = db.favoriteClipDao().getAllFavoritesList()
                if (favorites.isEmpty()) {
                    Toast.makeText(context, "אין משפטים במועדפים עדיין", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val clips = favorites.map { fav ->
                    SubtitleClip(
                        text = fav.text,
                        startTimeMs = fav.startTimeMs,
                        endTimeMs = fav.endTimeMs,
                        videoUri = Uri.fromFile(File(fav.videoPath))
                    )
                }
                
                vm.selectedVideoUri = clips.first().videoUri
                vm.practiceClips = clips.shuffled()
                vm.isQuizModeActive = true
                vm.isRandomModeActive = true 
                vm.navigateTo("video_player")
            }
        },
        onUploadVideo = { vm.navigateTo("word_deck") }, // Using this as Word Deck entry for now
        userName = vm.currentUser.name,
        totalXP = vm.currentUser.totalXP
    )
}

@Composable
fun BattleRouter(vm: MainViewModel, db: AppDatabase) {
    val context = LocalContext.current
    val exerciseVm: ExerciseViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        val linked = db.videoMetadataDao().getLinkedMetadata()
        val allClips = mutableListOf<SubtitleClip>()
        linked.forEach { meta ->
            val file = java.io.File(meta.filePath)
            val srt = com.example.lingoFlix.utils.FileUtils.findBestSrtForVideo(file)
            if (srt != null) {
                allClips.addAll(com.example.lingoFlix.utils.SrtParser.parseSrtFile(srt, Uri.fromFile(file)))
            }
        }
        if (allClips.isNotEmpty()) {
            exerciseVm.loadClips(allClips.shuffled().take(10))
        } else {
            Toast.makeText(context, "סמן סרטונים במאגר כדי להתחיל קרב!", Toast.LENGTH_SHORT).show()
            vm.navigateBack()
        }
    }
    
    ExerciseScreen(
        viewModel = exerciseVm,
        onBack = { vm.navigateBack() }
    )
}

@Composable
fun VideoListRouter(vm: MainViewModel, db: AppDatabase) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    val allMetadata by db.videoMetadataDao().getAllMetadata().collectAsState(initial = emptyList())
    val allFavorites by db.favoriteClipDao().getAllFavorites().collectAsState(initial = emptyList())
    
    val dirLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Handle picked directory logic here if needed
            LingoLog.i("MainContent", "Directory selected: $it")
        }
    }

    // Isolated router for Video List
    VideoListScreen(
        onVideoSelected = { uri ->
            vm.selectedVideoUri = uri
            vm.isQuizModeActive = false
            vm.practiceClips = null
            vm.navigateTo("video_player")
        },
        onPracticeRequested = { file, isQuiz ->
            val srt = com.example.lingoFlix.utils.FileUtils.findBestSrtForVideo(file)
            if (srt != null) {
                vm.selectedVideoUri = Uri.fromFile(file)
                vm.isQuizModeActive = isQuiz
                vm.practiceClips = com.example.lingoFlix.utils.SrtParser.parseSrtFile(srt, Uri.fromFile(file))
                vm.navigateTo("video_player")
            } else {
                // If no subtitles, just play regularly or show toast
                vm.selectedVideoUri = Uri.fromFile(file)
                vm.isQuizModeActive = false
                vm.practiceClips = null
                vm.navigateTo("video_player")
            }
        },
        onBack = { vm.navigateBack() },
        linkedVideos = allMetadata.filter { it.isLinked }.map { 
            try { java.io.File(it.filePath).relativeTo(java.io.File(context.filesDir, "videos")).path } catch(e: Exception) { it.filePath }
        }.toSet(),
        onToggleLink = { relativePath ->
            scope.launch {
                val fullPath = java.io.File(java.io.File(context.filesDir, "videos"), relativePath).absolutePath
                val meta = db.videoMetadataDao().getMetadataForVideo(fullPath) ?: VideoMetadata(fullPath)
                db.videoMetadataDao().insertMetadata(meta.copy(isLinked = !meta.isLinked))
            }
        },
        onToggleDifficulty = { vm.quizDifficulty = it },
        onSettingsRequested = { vm.navigateTo("settings") },
        metadataDao = db.videoMetadataDao(),
        onPickDirectory = { dirLauncher.launch(null) }
    )
}

@Composable
fun VideoPlayerRouter(vm: MainViewModel, db: AppDatabase) {
    val context = LocalContext.current
    val videoUri = vm.selectedVideoUri ?: return
    val scope = rememberCoroutineScope()
    val allFavorites by db.favoriteClipDao().getAllFavorites().collectAsState(initial = emptyList())

    VideoPlayerScreen(
        videoUri = videoUri,
        clips = vm.practiceClips,
        onBack = { vm.navigateBack() },
        isQuizMode = vm.isQuizModeActive,
        difficulty = vm.quizDifficulty,
        quizType = vm.quizType,
        userId = vm.currentUser.id,
        favoriteClips = allFavorites.map { it.id }.toSet(),
        onSaveWord = { word, trans ->
            scope.launch {
                val existing = db.vocabularyDao().getWord(word)
                if (existing == null) {
                    db.vocabularyDao().insertWord(com.example.lingoFlix.model.VocabularyWord(
                        word = word,
                        translation = trans,
                        contextSentence = vm.practiceClips?.get(0)?.text // Context from current session
                    ))
                }
            }
        },
        onCorrectAnswer = { points ->
            scope.launch {
                val statsManager = com.example.lingoFlix.data.UserStatsManager(context)
                statsManager.addXP(points)
                vm.currentUser = vm.currentUser.copy(totalXP = statsManager.getXP())
            }
        },
        onToggleFavorite = { clipId ->
            scope.launch {
                if (allFavorites.any { it.id == clipId }) {
                    db.favoriteClipDao().deleteById(clipId)
                } else {
                    val clip = vm.practiceClips?.find { "${it.startTimeMs}_${it.text.hashCode()}" == clipId }
                    if (clip != null) {
                        db.favoriteClipDao().insertFavorite(com.example.lingoFlix.model.FavoriteClip(
                            id = clipId,
                            videoPath = videoUri.path ?: "",
                            text = clip.text,
                            startTimeMs = clip.startTimeMs,
                            endTimeMs = clip.endTimeMs
                        ))
                    }
                }
            }
        }
    )
}

@Composable
fun SettingsRouter(vm: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsScreen(
        currentGeminiApiKey = SecurityUtils.getUserApiKey(context, vm.currentUser.id) ?: "",
        currentTmdbApiKey = SecurityUtils.getTmdbApiKey(context, vm.currentUser.id) ?: "",
        currentAnthropicApiKey = SecurityUtils.getAnthropicApiKey(context, vm.currentUser.id) ?: "",
        currentOpenAiApiKey = SecurityUtils.getOpenAiApiKey(context, vm.currentUser.id) ?: "",
        onSaveKeys = { gemini, tmdb, anthropic, openai ->
            SecurityUtils.saveUserApiKey(context, vm.currentUser.id, gemini)
            SecurityUtils.saveTmdbApiKey(context, vm.currentUser.id, tmdb)
            SecurityUtils.saveAnthropicApiKey(context, vm.currentUser.id, anthropic)
            SecurityUtils.saveOpenAiApiKey(context, vm.currentUser.id, openai)
        },
        onBack = { vm.navigateBack() }
    )
}
