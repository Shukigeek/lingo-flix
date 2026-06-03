package com.example.lingoFlix

import android.net.Uri
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.platform.LocalLifecycleOwner
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
        
        // Hide navigation bars, show status bar
        setImmersiveMode(window)

        setContent {
            LingoFlixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MainContent(this)
                }
            }
        }
    }

    private fun setImmersiveMode(window: android.view.Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Set behavior to transient so they hide automatically
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    }

    @Composable
    fun MainContent(activity: ComponentActivity) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // Saver for Set<String> and List<String>
        val setSaver = remember {
            Saver<MutableState<Set<String>>, ArrayList<String>>(
                save = { ArrayList(it.value.toList()) },
                restore = { mutableStateOf(it.toSet()) }
            )
        }
        val listSaver = remember {
            Saver<MutableState<List<String>>, ArrayList<String>>(
                save = { ArrayList(it.value) },
                restore = { mutableStateOf(it.toList()) }
            )
        }

        // Maintain immersive mode on resume
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    setImmersiveMode(activity.window)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    val statsManager = remember { UserStatsManager(context) }
    val sharedPrefs = remember { context.getSharedPreferences("lingo_prefs", android.content.Context.MODE_PRIVATE) }
    
    var navigationStack by rememberSaveable(saver = listSaver) { mutableStateOf(listOf("dashboard")) }
    val currentScreen = navigationStack.last()
    
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

    var currentUser by remember { mutableStateOf<UserProfile?>(UserProfile("main_user", "לומד", 0)) }
    
    var linkedToRandomPool by rememberSaveable(saver = setSaver) { 
        mutableStateOf(sharedPrefs.getStringSet("linked_videos", emptySet())?.toSet() ?: emptySet()) 
    }
    var favoriteClips by rememberSaveable(saver = setSaver) {
        mutableStateOf(sharedPrefs.getStringSet("favorite_clips", emptySet())?.toSet() ?: emptySet()) 
    }
    
    var totalXP by remember { mutableIntStateOf(statsManager.getXP()) }
    var currentStreak by remember { mutableIntStateOf(statsManager.getStreak()) }
    
    var selectedVideoFile by remember { mutableStateOf<File?>(null) }
    var currentVideoListDir by remember { mutableStateOf<File?>(null) }
    var selectedVideoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var practiceClips by rememberSaveable { mutableStateOf<List<SubtitleClip>?>(null) }
    var isQuizModeActive by rememberSaveable { mutableStateOf(false) }
    var isRandomModeActive by rememberSaveable { mutableStateOf(false) }
    var quizDifficulty by rememberSaveable { mutableStateOf("קל") }

    // Re-apply immersive mode when screen changes to ensure consistency
    LaunchedEffect(currentScreen) {
        setImmersiveMode(activity.window)
    }

    LaunchedEffect(linkedToRandomPool) {
        sharedPrefs.edit().putStringSet("linked_videos", HashSet(linkedToRandomPool)).apply()
    }
    LaunchedEffect(favoriteClips) {
        sharedPrefs.edit().putStringSet("favorite_clips", HashSet(favoriteClips)).apply()
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val videoUris = uris.filter { uri ->
                val type = context.contentResolver.getType(uri)
                val name = FileUtils.getFileName(context, uri) ?: ""
                type?.startsWith("video/") == true || 
                listOf("mp4", "mkv", "avi", "mov", "webm").any { name.endsWith(".$it", ignoreCase = true) }
            }
            val srtUris = uris.filter { uri ->
                val type = context.contentResolver.getType(uri)
                val name = FileUtils.getFileName(context, uri) ?: ""
                type?.contains("subrip") == true || 
                type?.contains("application/octet-stream") == true || 
                type?.contains("text/plain") == true ||
                name.endsWith(".srt", ignoreCase = true)
            }

            if (videoUris.isEmpty() && uris.size == 1) {
                // If user picked one file and it doesn't have a video mime type, 
                // but they meant it to be a video 
                val uri = uris[0]
                val name = FileUtils.getFileName(context, uri) ?: "video_${System.currentTimeMillis()}.mp4"
                val savedFile = FileUtils.saveVideoToInternalStorage(context, uri, name)
                if (savedFile != null) {
                    Toast.makeText(context, "סרטון $name נשמר!", Toast.LENGTH_SHORT).show()
                }
            } else {
                videoUris.forEach { vUri ->
                    val vName = FileUtils.getFileName(context, vUri) ?: "video_${System.currentTimeMillis()}.mp4"
                    val savedVideo = FileUtils.saveVideoToInternalStorage(context, vUri, vName)
                    
                    if (savedVideo != null) {
                        // Automatically link to random pool using relative path
                        val videoDir = File(context.filesDir, "videos")
                        val relativePath = savedVideo.relativeTo(videoDir).path
                        linkedToRandomPool = linkedToRandomPool + relativePath

                        // Look for a matching SRT in the selected URIs
                        val vBase = vName.substringBeforeLast(".")
                        val matchingSrt = srtUris.find { sUri ->
                            val sName = FileUtils.getFileName(context, sUri) ?: ""
                            sName.contains(vBase, ignoreCase = true) || (vBase.isNotEmpty() && sName.substringBeforeLast(".").contains(vBase, ignoreCase = true))
                        } ?: if (srtUris.size == 1 && videoUris.size == 1) srtUris[0] else null

                        matchingSrt?.let { sUri ->
                            val sName = FileUtils.getFileName(context, sUri) ?: "$vBase.srt"
                            FileUtils.saveSubtitleToInternalStorage(context, sUri, sName, vName)
                            Toast.makeText(context, "סרטון וכתוביות עבור $vBase נשמרו!", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(context, "סרטון $vName נשמר!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                // If user picked only SRTs
                if (videoUris.isEmpty() && srtUris.isNotEmpty()) {
                    srtUris.forEach { sUri ->
                        val sName = FileUtils.getFileName(context, sUri) ?: "subtitle_${System.currentTimeMillis()}.srt"
                        FileUtils.saveSubtitleToInternalStorage(context, sUri, sName)
                    }
                    Toast.makeText(context, "כתוביות נשמרו!", Toast.LENGTH_SHORT).show()
                }
            }

            // Force refresh if we are in video list
            if (currentScreen == "video_list") {
                navigateBack()
                navigateTo("video_list")
            }
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        val lastScreen = navigationStack.last()
        navigateBack()
        if (lastScreen == "player") {
            practiceClips = null
            isQuizModeActive = false
            isRandomModeActive = false
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
            alpha = 0.5f
        )

        // Wrapper for content to handle system bars padding
        // statusBarsPadding() ensures content is below the status bar
        // navigationBarsPadding() is not used here because we want immersive (transient) navigation
        Box(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()) {
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

                            val srtFile = FileUtils.findBestSrtForVideo(file)
                            Log.d("MainActivity", "Found SRT file: ${srtFile?.absolutePath}")

                            if (srtFile != null && srtFile.exists()) {
                                val clips = SrtParser.parseSrtFile(srtFile, Uri.fromFile(file))
                                Log.d("MainActivity", "Parsed ${clips.size} clips")
                                if (clips.isNotEmpty()) {
                                    practiceClips = clips 
                                    selectedVideoUri = Uri.fromFile(file)
                                    isQuizModeActive = true
                                    navigateTo("player")
                                    Log.d("MainActivity", "Switching to player screen")
                                } else {
                                    Toast.makeText(context, "לא נמצאו כתוביות תקינות", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Just play the video if no SRT
                                selectedVideoUri = Uri.fromFile(file)
                                isQuizModeActive = false
                                navigateTo("player")
                            }
                        },
                        onRegularView = {
                            file.setLastModified(System.currentTimeMillis())
                            selectedVideoUri = Uri.fromFile(file)
                            practiceClips = null
                            isQuizModeActive = false
                            navigateTo("player")
                        },
                        onBack = { navigateBack() }
                    )
                }
            }

            "settings" -> {
                SettingsScreen(
                    currentApiKey = currentUser?.let { SecurityUtils.getUserApiKey(context, it.id) } ?: "",
                    onSaveApiKey = { newKey ->
                        currentUser?.let { SecurityUtils.saveUserApiKey(context, it.id, newKey) }
                        navigateBack()
                    },
                    onBack = { navigateBack() }
                )
            }

            "dashboard" -> {
                DashboardScreen(
                    onMyVideos = { navigateTo("video_list") },
                    onUploadVideo = { pickVideoLauncher.launch(arrayOf("video/*", "application/x-subrip", "text/plain", "application/octet-stream")) },
                    onRandomSentences = { showDifficultyDialogForRandom = true },
                    onFavorites = { showDifficultyDialogForFavorites = true },
                    onVideoSelected = { file ->
                        selectedVideoFile = file
                        navigateTo("difficulty")
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
                            
                            // Create a list of ALL available videos for discovery
                            val allVideos = mutableListOf<File>()
                            
                            // 1. Collect ALL video files recursively
                            val discoveredVideos = mutableListOf<File>()
                            videoDir.walkTopDown().forEach { file ->
                                if (!file.isDirectory && listOf("mp4", "mkv", "avi", "mov", "webm").any { file.name.endsWith(".$it", ignoreCase = true) }) {
                                    discoveredVideos.add(file)
                                }
                            }

                            // 2. Filter videos that are explicitly linked in the preferences
                            // We match by relative path since linkedToRandomPool now stores relative paths
                            val linkedPaths = linkedToRandomPool
                            
                            var filteredVideos = discoveredVideos.filter { linkedPaths.contains(it.relativeTo(videoDir).path) }
                            
                            // Fallback: If nothing is linked, use all videos with SRTs to avoid empty pool
                            if (filteredVideos.isEmpty()) {
                                filteredVideos = discoveredVideos.filter { FileUtils.findBestSrtForVideo(it)?.exists() == true }
                            }
                            
                            allVideos.addAll(filteredVideos)

                            var videosWithSrt = 0
                            var linkedAndMatched = 0
                            
                            // For each video, find its best matching SRT
                            allVideos.forEach { videoFile ->
                                val bestSrt = FileUtils.findBestSrtForVideo(videoFile)
                                if (bestSrt != null && bestSrt.exists()) {
                                    videosWithSrt++
                                    val clips = SrtParser.parseSrtFile(bestSrt, Uri.fromFile(videoFile))
                                    if (clips.isNotEmpty()) {
                                        linkedAndMatched++
                                        allClips.addAll(clips)
                                    }
                                }
                            }

                            if (allClips.isNotEmpty()) {
                                val linkedMsg = if (linkedToRandomPool.isEmpty()) "מציג משפטים מכל הסרטונים (כי המאגר הקיים ריק)" else "נמצאו ${allClips.size} משפטים מתוך $linkedAndMatched סרטונים"
                                Toast.makeText(context, linkedMsg, Toast.LENGTH_SHORT).show()
                                practiceClips = allClips.shuffled()
                                selectedVideoUri = practiceClips!![0].videoUri
                                isQuizModeActive = true
                                isRandomModeActive = true
                                navigateTo("player")
                            } else {
                                val msg = when {
                                    discoveredVideos.isEmpty() -> "לא נמצאו סרטונים בתיקייה. העלה סרטון כדי להתחיל!"
                                    linkedToRandomPool.isEmpty() -> "אין סרטונים עם כתוביות במאגר. אנא וודא שיש קבצי SRT לסרטונים שלך."
                                    allVideos.isEmpty() -> "הסרטונים שסימנת לא נמצאו בתיקייה."
                                    videosWithSrt == 0 -> "נמצאו סרטונים, אך לאף אחד מהם אין קובץ כתוביות (SRT) תואם."
                                    else -> "נמצאו כתוביות, אך לא הצלחנו לקרוא מהן משפטים."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
                                    }.firstOrNull { it.exists() }
                                    
                                    if (videoFile != null) {
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
                            }

                            if (favClipsList.isNotEmpty()) {
                                Toast.makeText(context, "נמצאו ${favClipsList.size} משפטים מועדפים", Toast.LENGTH_SHORT).show()
                                practiceClips = favClipsList.shuffled()
                                selectedVideoUri = practiceClips!![0].videoUri
                                isQuizModeActive = true
                                isRandomModeActive = true
                                navigateTo("player")
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
                            navigateBack()
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
                            navigateTo("difficulty")
                        } else {
                            selectedVideoUri = uri
                            practiceClips = null
                            isQuizModeActive = false
                            navigateTo("player")
                        }
                    },
                    onPracticeRequested = { videoFile, isQuiz ->
                        if (isQuiz) {
                            selectedVideoFile = videoFile
                            navigateTo("difficulty")
                        } else {
                            videoFile.setLastModified(System.currentTimeMillis())
                            selectedVideoUri = Uri.fromFile(videoFile)
                            practiceClips = null
                            isQuizModeActive = false
                            navigateTo("player")
                        }
                    },
                    onBack = { navigateBack() },
                    linkedVideos = linkedToRandomPool,
                    onToggleLink = { relativePath ->
                        val isNowLinked = !linkedToRandomPool.contains(relativePath)
                        linkedToRandomPool = if (isNowLinked) {
                            linkedToRandomPool + relativePath
                        } else {
                            linkedToRandomPool - relativePath
                        }
                        val msg = if (isNowLinked) "נוסף למאגר הרנדומלי" else "הוסר מהמאגר הרנדומלי"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onToggleDifficulty = { quizDifficulty = it },
                    onSettingsRequested = { navigateTo("settings") },
                    favoriteClips = favoriteClips,
                    onToggleFavorite = { clipId ->
                        favoriteClips = if (favoriteClips.contains(clipId)) {
                            favoriteClips - clipId
                        } else {
                            favoriteClips + clipId
                        }
                    },
                    userId = currentUser?.id ?: "guest",
                    initialDir = currentVideoListDir,
                    onDirChanged = { currentVideoListDir = it }
                )
            }
        }
    }
}
}
}
