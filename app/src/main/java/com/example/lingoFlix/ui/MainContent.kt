package com.example.lingoFlix.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.ui.navigation.BottomNavigationBar
import com.example.lingoFlix.ui.viewmodel.MainViewModel
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SecurityUtils
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.model.VideoMetadata
import kotlinx.coroutines.launch

@Composable
fun MainContent(
    activity: ComponentActivity, 
    database: AppDatabase,
    mainViewModel: MainViewModel = viewModel()
) {
    val navigationStack = mainViewModel.navigationStack
    val currentScreen = navigationStack.lastOrNull() ?: "dashboard"

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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "dashboard" -> DashboardRouter(mainViewModel)
                "discovery" -> DiscoveryScreen(mainViewModel.currentUser.id)
                "video_list" -> VideoListRouter(mainViewModel, database)
                "video_player" -> VideoPlayerRouter(mainViewModel, database)
                "settings" -> SettingsRouter(mainViewModel)
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
fun DashboardRouter(vm: MainViewModel) {
    // Isolated router for Dashboard
    DashboardScreen(
        onMyVideos = { vm.navigateTo("video_list") },
        onAdminClick = { vm.navigateTo("admin_dashboard") },
        userName = vm.currentUser.name
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
        onSaveKeys = { gemini, tmdb, anthropic ->
            SecurityUtils.saveUserApiKey(context, vm.currentUser.id, gemini)
            SecurityUtils.saveTmdbApiKey(context, vm.currentUser.id, tmdb)
            SecurityUtils.saveAnthropicApiKey(context, vm.currentUser.id, anthropic)
        },
        onBack = { vm.navigateBack() }
    )
}
