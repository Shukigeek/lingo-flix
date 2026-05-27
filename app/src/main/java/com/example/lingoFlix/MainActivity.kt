package com.example.lingoFlix

import android.net.Uri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.ui.DashboardScreen
import com.example.lingoFlix.ui.DifficultyScreen
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import com.example.lingoFlix.data.UserStatsManager
import com.example.lingoFlix.model.UserProfile
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.ui.SettingsScreen
import com.example.lingoFlix.ui.VideoListScreen
import com.example.lingoFlix.ui.VideoPlayerScreen
import com.example.lingoFlix.ui.components.DifficultySelectionDialog
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.SecurityUtils
import com.example.lingoFlix.utils.SrtParser
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide navigation bars, show status bar
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())

        setContent {
            LingoFlixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    val context = LocalContext.current
    val statsManager = remember { UserStatsManager(context) }
    val sharedPrefs = remember { context.getSharedPreferences("lingo_prefs", android.content.Context.MODE_PRIVATE) }
    
    var currentScreen by rememberSaveable { mutableStateOf("dashboard") }
    var currentUser by remember { mutableStateOf<UserProfile?>(UserProfile("main_user", "לומד", 0)) }
    
    var linkedToRandomPool by rememberSaveable { 
        mutableStateOf(sharedPrefs.getStringSet("linked_videos", emptySet()) ?: emptySet()) 
    }
    var favoriteClips by rememberSaveable { 
        mutableStateOf(sharedPrefs.getStringSet("favorite_clips", emptySet()) ?: emptySet()) 
    }
    
    var totalXP by remember { mutableIntStateOf(statsManager.getXP()) }
    var currentStreak by remember { mutableIntStateOf(statsManager.getStreak()) }
    
    var selectedVideoFile by remember { mutableStateOf<File?>(null) }
    var selectedVideoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var practiceClips by rememberSaveable { mutableStateOf<List<SubtitleClip>?>(null) }
    var isQuizModeActive by rememberSaveable { mutableStateOf(false) }
    var isRandomModeActive by rememberSaveable { mutableStateOf(false) }
    var quizDifficulty by rememberSaveable { mutableStateOf("קל") }

    LaunchedEffect(linkedToRandomPool) {
        sharedPrefs.edit().putStringSet("linked_videos", linkedToRandomPool).apply()
    }
    LaunchedEffect(favoriteClips) {
        sharedPrefs.edit().putStringSet("favorite_clips", favoriteClips).apply()
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val originalName = FileUtils.getFileName(context, uri) ?: "video_${System.currentTimeMillis()}.mp4"
            val savedFile = FileUtils.saveVideoToInternalStorage(context, uri, originalName)
            if (savedFile != null) {
                Toast.makeText(context, "סרטון $originalName נשמר!", Toast.LENGTH_SHORT).show()
                // Force refresh if we are in video list
                if (currentScreen == "video_list") {
                    currentScreen = "dashboard"
                    currentScreen = "video_list"
                }
            }
        }
    }

    BackHandler(enabled = currentScreen != "dashboard") {
        when (currentScreen) {
            "player" -> {
                currentScreen = "video_list"
                practiceClips = null
                isQuizModeActive = false
            }
            "difficulty" -> {
                currentScreen = "dashboard"
            }
            "video_list", "settings", "favorites" -> {
                currentScreen = "dashboard"
            }
        }
    }

    var showDifficultyDialogForRandom by remember { mutableStateOf(false) }
    var showDifficultyDialogForFavorites by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // App Background Image (Friends)
        Image(
            painter = painterResource(id = R.drawable.friends),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f // Control transparency of the image itself here
        )

        when (currentScreen) {
            "difficulty" -> {
                selectedVideoFile?.let { file ->
                    DifficultyScreen(
                        videoTitle = file.name,
                        onDifficultySelected = { difficulty ->
                            quizDifficulty = when(difficulty) {
                                com.example.lingoFlix.util.GameLogic.Difficulty.EASY -> "קל"
                                com.example.lingoFlix.util.GameLogic.Difficulty.MEDIUM -> "בינוני"
                                com.example.lingoFlix.util.GameLogic.Difficulty.HARD -> "קשה"
                            }
                            
                            // Update last modified to keep it in recents
                            file.setLastModified(System.currentTimeMillis())

                            val videoName = file.nameWithoutExtension.lowercase()
                            val videoDir = file.parentFile
                            val srtFile = videoDir?.listFiles()?.find {
                                it.extension.lowercase() == "srt" && 
                                it.nameWithoutExtension.lowercase() == videoName 
                            } ?: File(videoDir, "${file.nameWithoutExtension}.srt")

                            if (srtFile.exists()) {
                                val clips = SrtParser.parseSrtFile(srtFile, Uri.fromFile(file))
                                if (clips.isNotEmpty()) {
                                    practiceClips = clips 
                                    selectedVideoUri = Uri.fromFile(file)
                                    isQuizModeActive = true
                                    currentScreen = "player"
                                } else {
                                    Toast.makeText(context, "לא נמצאו כתוביות תקינות", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Just play the video if no SRT
                                selectedVideoUri = Uri.fromFile(file)
                                isQuizModeActive = false
                                currentScreen = "player"
                            }
                        },
                        onRegularView = {
                            file.setLastModified(System.currentTimeMillis())
                            selectedVideoUri = Uri.fromFile(file)
                            practiceClips = null
                            isQuizModeActive = false
                            currentScreen = "player"
                        },
                        onBack = { currentScreen = "dashboard" }
                    )
                }
            }

            "settings" -> {
                SettingsScreen(
                    currentApiKey = currentUser?.let { SecurityUtils.getUserApiKey(context, it.id) } ?: "",
                    onSaveApiKey = { newKey ->
                        currentUser?.let { SecurityUtils.saveUserApiKey(context, it.id, newKey) }
                        currentScreen = "dashboard"
                    },
                    onBack = { currentScreen = "dashboard" }
                )
            }

            "dashboard" -> {
                DashboardScreen(
                    onMyVideos = { currentScreen = "video_list" },
                    onUploadVideo = { pickVideoLauncher.launch("video/*") },
                    onRandomSentences = { showDifficultyDialogForRandom = true },
                    onFavorites = { showDifficultyDialogForFavorites = true },
                    onVideoSelected = { file ->
                        selectedVideoFile = file
                        currentScreen = "difficulty"
                    },
                    totalXP = totalXP,
                    currentStreak = currentStreak,
                    userName = currentUser?.name ?: "לומד"
                )
                
                if (showDifficultyDialogForRandom) {
                    DifficultySelectionDialog(
                        onDismiss = { showDifficultyDialogForRandom = false },
                        onStart = { diff ->
                            quizDifficulty = diff
                            showDifficultyDialogForRandom = false
                            
                            val videoDir = File(context.filesDir, "videos")
                            val allClips = mutableListOf<SubtitleClip>()
                            
                            // Recursively find all SRT files in the video directory
                            videoDir.walkTopDown().forEach { file ->
                                if (!file.isDirectory && file.extension.lowercase() == "srt") {
                                    // Try to find the corresponding video file with common extensions
                                    val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "webm")
                                    val videoFile = videoExtensions.map { ext -> 
                                        File(file.parentFile, "${file.nameWithoutExtension}.$ext") 
                                    }.firstOrNull { it.exists() } ?: File(file.parentFile, "${file.nameWithoutExtension}.mp4")
                                    
                                    if (linkedToRandomPool.contains(videoFile.name) || 
                                        linkedToRandomPool.contains(file.nameWithoutExtension)) {
                                        val videoUri = Uri.fromFile(videoFile)
                                        allClips.addAll(SrtParser.parseSrtFile(file, videoUri))
                                    }
                                }
                            }

                            if (allClips.isNotEmpty()) {
                                practiceClips = allClips.shuffled()
                                selectedVideoUri = practiceClips!![0].videoUri
                                isQuizModeActive = true
                                isRandomModeActive = true
                                currentScreen = "player"
                            } else {
                                Toast.makeText(context, "קודם צריך לקשר סרטונים עם כתוביות למאגר", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                if (showDifficultyDialogForFavorites) {
                    DifficultySelectionDialog(
                        onDismiss = { showDifficultyDialogForFavorites = false },
                        onStart = { diff ->
                            quizDifficulty = diff
                            showDifficultyDialogForFavorites = false
                            
                            val videoDir = File(context.filesDir, "videos")
                            val favClipsList = mutableListOf<SubtitleClip>()
                            
                            // Walk through all directories and find all SRT files
                            videoDir.walkTopDown().forEach { file ->
                                if (!file.isDirectory && file.extension.lowercase() == "srt") {
                                    val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "webm")
                                    val videoFile = videoExtensions.map { ext -> 
                                        File(file.parentFile, "${file.nameWithoutExtension}.$ext") 
                                    }.firstOrNull { it.exists() } ?: File(file.parentFile, "${file.nameWithoutExtension}.mp4")
                                    
                                    val clips = SrtParser.parseSrtFile(file, Uri.fromFile(videoFile))
                                    clips.forEach { clip ->
                                        // Standardize clipId format
                                        val clipId = "${videoFile.name}|${clip.startTimeMs}"
                                        if (favoriteClips.contains(clipId)) {
                                            favClipsList.add(clip)
                                        }
                                    }
                                }
                            }

                            if (favClipsList.isNotEmpty()) {
                                practiceClips = favClipsList.shuffled()
                                selectedVideoUri = practiceClips!![0].videoUri
                                isQuizModeActive = true
                                isRandomModeActive = true
                                currentScreen = "player"
                            } else {
                                Toast.makeText(context, "עדיין לא שמרת משפטים מועדפים!", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }

            "player" -> {
                selectedVideoUri?.let { uri ->
                    VideoPlayerScreen(
                        videoUri = uri,
                        clips = practiceClips,
                        onBack = {
                            currentScreen = "video_list"
                            practiceClips = null
                            isQuizModeActive = false
                            isRandomModeActive = false
                        },
                        favoriteClips = favoriteClips,
                        onToggleFavorite = { clipId ->
                            favoriteClips = if (favoriteClips.contains(clipId)) {
                                favoriteClips - clipId
                            } else {
                                favoriteClips + clipId
                            }
                        },
                        isQuizMode = isQuizModeActive,
                        isRandomMode = isRandomModeActive,
                        difficulty = quizDifficulty,
                        onCorrectAnswer = {
                            val points = when (quizDifficulty) {
                                "בינוני" -> 30
                                "קשה" -> 40
                                else -> 20 // קל
                            }
                            statsManager.addXP(points)
                            statsManager.markActivityToday()
                            totalXP = statsManager.getXP()
                            currentStreak = statsManager.getStreak()
                        },
                        userId = currentUser?.id ?: "guest"
                    )
                }
            }

            "video_list" -> {
                VideoListScreen(
                    onVideoSelected = { uri ->
                        if (uri.scheme == "file") {
                            selectedVideoFile = File(uri.path!!)
                            currentScreen = "difficulty"
                        } else {
                            selectedVideoUri = uri
                            practiceClips = null
                            isQuizModeActive = false
                            currentScreen = "player"
                        }
                    },
                    onPracticeRequested = { videoFile, isQuiz ->
                        if (isQuiz) {
                            selectedVideoFile = videoFile
                            currentScreen = "difficulty"
                        } else {
                            videoFile.setLastModified(System.currentTimeMillis())
                            selectedVideoUri = Uri.fromFile(videoFile)
                            practiceClips = null
                            isQuizModeActive = false
                            currentScreen = "player"
                        }
                    },
                    onBack = { currentScreen = "dashboard" },
                    linkedVideos = linkedToRandomPool,
                    onToggleLink = { fileName ->
                        linkedToRandomPool = if (linkedToRandomPool.contains(fileName)) {
                            linkedToRandomPool - fileName
                        } else {
                            linkedToRandomPool + fileName
                        }
                    },
                    onToggleDifficulty = { quizDifficulty = it },
                    onSettingsRequested = { currentScreen = "settings" },
                    userId = currentUser?.id ?: "guest"
                )
            }
        }
    }
}
