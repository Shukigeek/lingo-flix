package com.example.lingoFlix

import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.lingoFlix.ui.DashboardScreen
import com.example.lingoFlix.ui.theme.LingoFlixTheme
import com.example.lingoFlix.data.UserStatsManager
import android.content.Intent
import android.os.Parcelable
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

data class SubtitleClip(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val videoUri: Uri
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LingoFlixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
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
    
    // Initialize states from SharedPrefs immediately
    var linkedToRandomPool by rememberSaveable { 
        mutableStateOf(sharedPrefs.getStringSet("linked_videos", emptySet()) ?: emptySet()) 
    }
    var favoriteClips by rememberSaveable { 
        mutableStateOf(sharedPrefs.getStringSet("favorite_clips", emptySet()) ?: emptySet()) 
    }
    
    var totalXP by remember { mutableIntStateOf(statsManager.getXP()) }
    var currentStreak by remember { mutableIntStateOf(statsManager.getStreak()) }
    
    var selectedVideoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var showVideoList by rememberSaveable { mutableStateOf(false) }
    var practiceClips by rememberSaveable { mutableStateOf<List<SubtitleClip>?>(null) }
    var isQuizModeActive by rememberSaveable { mutableStateOf(false) }
    var quizDifficulty by rememberSaveable { mutableStateOf("קל") }

    // Persist state when it changes
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
            val fileName = getFileName(context, uri) ?: "video_${System.currentTimeMillis()}.mp4"
            val savedFile = saveVideoToInternalStorage(context, uri, fileName)
            if (savedFile != null) {
                Toast.makeText(context, "סרטון $fileName נשמר!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle system back button
    BackHandler(enabled = isPlaying || showVideoList) {
        if (isPlaying) {
            isPlaying = false
            practiceClips = null
            isQuizModeActive = false
        } else if (showVideoList) {
            showVideoList = false
        }
    }

    var showDifficultyDialogForRandom by remember { mutableStateOf(false) }
    var showDifficultyDialogForFavorites by remember { mutableStateOf(false) }

    if (showDifficultyDialogForRandom) {
        DifficultySelectionDialog(
            onDismiss = { showDifficultyDialogForRandom = false },
            onStart = { diff ->
                quizDifficulty = diff
                showDifficultyDialogForRandom = false
                
                // Logic to start random sentences
                val videoDir = File(context.filesDir, "videos")
                val allClips = mutableListOf<SubtitleClip>()
                linkedToRandomPool.forEach { fileName ->
                    val videoFile = File(videoDir, fileName)
                    val srtFile = File(videoDir, "${videoFile.nameWithoutExtension}.srt")
                    if (srtFile.exists()) {
                        allClips.addAll(parseSrtFile(srtFile, Uri.fromFile(videoFile)))
                    }
                }

                if (allClips.isNotEmpty()) {
                    practiceClips = allClips.shuffled()
                    selectedVideoUri = practiceClips!![0].videoUri
                    isQuizModeActive = true
                    isPlaying = true
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
                
                // Logic to start favorites
                val videoDir = File(context.filesDir, "videos")
                val favClipsList = mutableListOf<SubtitleClip>()
                val videoFiles = videoDir.listFiles()?.filter { it.extension != "srt" } ?: emptyList()
                videoFiles.forEach { videoFile ->
                    val srtFile = File(videoDir, "${videoFile.nameWithoutExtension}.srt")
                    if (srtFile.exists()) {
                        val clips = parseSrtFile(srtFile, Uri.fromFile(videoFile))
                        clips.forEach { clip ->
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
                    isPlaying = true
                } else {
                    Toast.makeText(context, "עדיין לא שמרת משפטים מועדפים!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (isPlaying && selectedVideoUri != null) {
        VideoPlayerScreen(
            videoUri = selectedVideoUri!!,
            clips = practiceClips,
            onBack = {
                isPlaying = false
                practiceClips = null
                isQuizModeActive = false
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
            }
        )
    } else if (showVideoList) {
        VideoListScreen(
            onVideoSelected = { uri ->
                selectedVideoUri = uri
                isPlaying = true
            },
            onPracticeRequested = { file, isQuiz ->
                val srtFile = File(file.parentFile, "${file.nameWithoutExtension}.srt")
                if (srtFile.exists()) {
                    val clips = parseSrtFile(srtFile, Uri.fromFile(file))
                    if (clips.isNotEmpty()) {
                        practiceClips = clips 
                        selectedVideoUri = Uri.fromFile(file)
                        isQuizModeActive = isQuiz
                        isPlaying = true
                    } else {
                        Toast.makeText(context, "לא הצלחתי לקרוא את המשפטים", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "אין כתוביות לסרטון הזה", Toast.LENGTH_SHORT).show()
                }
            },
            onBack = { showVideoList = false },
            linkedVideos = linkedToRandomPool,
            onToggleLink = { fileName ->
                linkedToRandomPool = if (linkedToRandomPool.contains(fileName)) {
                    linkedToRandomPool - fileName
                } else {
                    linkedToRandomPool + fileName
                }
            },
            onToggleDifficulty = { quizDifficulty = it }
        )
    } else {
        DashboardScreen(
            onMyVideos = {
                showVideoList = true
            },
            onUploadVideo = {
                pickVideoLauncher.launch("video/*")
            },
            onRandomSentences = {
                showDifficultyDialogForRandom = true
            },
            onFavorites = {
                showDifficultyDialogForFavorites = true
            },
            totalXP = totalXP,
            currentStreak = currentStreak
        )
    }
}

@Composable
fun DifficultySelectionDialog(onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var selectedDifficulty by remember { mutableStateOf("קל") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחר רמת קושי לתרגול") },
        text = {
            Column {
                listOf("קל", "בינוני", "קשה").forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDifficulty = level }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                        Text(text = level, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    text = when(selectedDifficulty) {
                        "קל" -> "מילה אחת חסרה בכל משפט"
                        "בינוני" -> "כ-40% מהמילים יוסתרו"
                        else -> "רוב המילים יוסתרו"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selectedDifficulty) }) {
                Text("התחל תרגול")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול")
            }
        }
    )
}

fun getFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) name = it.getString(nameIndex)
        }
    }
    return name
}

fun saveVideoToInternalStorage(context: android.content.Context, uri: Uri, fileName: String): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val videoDir = File(context.filesDir, "videos")
        if (!videoDir.exists()) videoDir.mkdirs()
        
        val targetFile = File(videoDir, fileName)
        val outputStream = FileOutputStream(targetFile)
        
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    } catch (e: Exception) {
        Log.e("MainActivity", "Error saving video", e)
        null
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUri: Uri, 
    clips: List<SubtitleClip>? = null,
    onBack: () -> Unit,
    favoriteClips: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    isQuizMode: Boolean = false,
    difficulty: String = "קל",
    onCorrectAnswer: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullScreen by remember { mutableStateOf(false) }
    var currentClipIndex by remember { mutableIntStateOf(0) }
    
    // Quiz State
    var userInput by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }
    var hiddenIndices by remember(currentClipIndex) { mutableStateOf(setOf<Int>()) }
    var wordsList by remember(currentClipIndex) { mutableStateOf(listOf<String>()) }
    
    // Confetti State
    var confettiState by remember { mutableStateOf<List<Party>>(emptyList()) }
    
    // X-Ray State
    var showXRay by remember { mutableStateOf(false) }
    var xRayLine by remember { mutableStateOf("") }
    
    val videoFileName = remember(videoUri) {
        if (videoUri.scheme == "file") File(videoUri.path!!).name else "unknown"
    }

    // Prepare quiz for current clip
    LaunchedEffect(currentClipIndex, clips, isQuizMode) {
        if (isQuizMode && clips != null) {
            val originalText = clips[currentClipIndex].text
            // Split by spaces and punctuation
            val words = originalText.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;])|(?=[.,!?;])")).filter { it.isNotBlank() }
            wordsList = words
            
            val validIndices = words.indices.filter { words[it].length > 1 && words[it].any { c -> c.isLetter() } }
            
            if (validIndices.isNotEmpty()) {
                val countToHide = when (difficulty) {
                    "בינוני" -> (validIndices.size * 0.4).toInt().coerceAtLeast(1)
                    "קשה" -> (validIndices.size * 0.7).toInt().coerceAtLeast(1)
                    else -> 1 // קל
                }
                hiddenIndices = validIndices.shuffled().take(countToHide).toSet()
            }
            userInput = ""
            isChecked = false
        }
    }
    
    // Always load clips if possible for favoriting
    val allClipsForThisVideo = remember(videoUri) {
        if (videoUri.scheme == "file") {
            val videoFile = File(videoUri.path!!)
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            if (srtFile.exists()) parseSrtFile(srtFile, videoUri) else emptyList()
        } else emptyList()
    }

    val exoPlayer = remember(videoUri) {
        val videoFile = if (videoUri.scheme == "file") File(videoUri.path!!) else null
        val srtFile = videoFile?.let { File(it.parentFile, "${it.nameWithoutExtension}.srt") }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUri)

        // Show subtitles via ExoPlayer ONLY if NOT in custom clips mode
        if (srtFile != null && srtFile.exists() && clips == null) {
            val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(srtFile))
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("he")
                .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
        }

        ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItemBuilder.build())
            prepare()
            playWhenReady = true
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage("he")
                .build()
        }
    }
    
    // Track current position for favoriting in regular mode
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    
    if (clips == null && allClipsForThisVideo.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                currentPlaybackPosition = exoPlayer.currentPosition
                kotlinx.coroutines.delay(500)
            }
        }
    }

    // Logic for playing specific clips
    LaunchedEffect(clips, currentClipIndex) {
        clips?.let {
            val clip = it[currentClipIndex]
            
            // Check if we need to change the video source
            if (exoPlayer.currentMediaItem?.localConfiguration?.uri != clip.videoUri) {
                val mediaItemBuilder = MediaItem.Builder().setUri(clip.videoUri)
                
                // Add subtitles if not in quiz/clips mode (only for manual playback)
                // In random/clips mode, we manage subtitles via our own UI
                
                exoPlayer.setMediaItem(mediaItemBuilder.build())
                exoPlayer.prepare()
            }

            exoPlayer.seekTo(clip.startTimeMs)
            exoPlayer.play()
        }
    }

    // Check if clip ended
    if (clips != null) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(100)
                val clip = clips[currentClipIndex]
                if (exoPlayer.currentPosition >= clip.endTimeMs) {
                    exoPlayer.pause()
                }
            }
        }
    }

    // Pause player when app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isFullScreen) {
        activity?.let {
            val window = it.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (showXRay) {
        XRayDialog(
            line = xRayLine,
            onDismiss = { showXRay = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = clips == null // Hide default controls in practice mode
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (confettiState.isNotEmpty()) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = confettiState
            )
        }
        
        // Custom HUD
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                IconButton(
                    onClick = { isFullScreen = !isFullScreen },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = Color.White
                    )
                }
            }

            if (clips != null) {
                // Progress Bar for Quiz
                LinearProgressIndicator(
                    progress = { (currentClipIndex + 1).toFloat() / clips.size },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Text(
                    text = "משפט ${currentClipIndex + 1} מתוך ${clips.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                val currentClip = clips[currentClipIndex]
                val clipId = "$videoFileName|${currentClip.startTimeMs}"
                val isFavorite = favoriteClips.contains(clipId)

                // Show Current Sentence with Glassmorphism and Animated Border
                val infiniteTransition = rememberInfiniteTransition(label = "border")
                val borderColor by infiniteTransition.animateColor(
                    initialValue = MaterialTheme.colorScheme.primary,
                    targetValue = MaterialTheme.colorScheme.tertiary,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "borderColor"
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val text = currentClip.text
                    val isHebrew = text.any { it in '\u0590'..'\u05FF' }
                    
                    Column(modifier = Modifier.padding(20.dp)) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides (if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr)
                        ) {
                            if (isQuizMode && hiddenIndices.isNotEmpty()) {
                                // Display text with blank
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    wordsList.forEachIndexed { index, word ->
                                        if (hiddenIndices.contains(index)) {
                                            val isWordCorrect = userInput.split(Regex("\\s+")).any { it.trim().equals(word.trim(), ignoreCase = true) }
                                            Text(
                                                text = if (isChecked) word else "____",
                                                color = if (!isChecked) Color.Yellow else if (isWordCorrect) Color.Green else Color.Red,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            )
                                        } else {
                                            Text(
                                                text = word,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = text,
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Favorite button directly below the sentence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(clipId) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) Color(0xFFFFC107) else Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    IconButton(
                        onClick = { 
                            xRayLine = currentClip.text
                            showXRay = true 
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = "Subtitle X-Ray",
                            tint = Color.White
                        )
                    }
                }

                if (isQuizMode && !isChecked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = { Text("הקלד את המילים החסרות...") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isChecked = true }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Check")
                            }
                        }
                    )
                } else if (isQuizMode && isChecked) {
                    val hiddenWords = wordsList.filterIndexed { index, _ -> hiddenIndices.contains(index) }.joinToString(" ")
                    val allCorrect = hiddenIndices.all { idx ->
                        userInput.split(Regex("\\s+")).any { it.trim().equals(wordsList[idx].trim(), ignoreCase = true) }
                    }
                    
                    LaunchedEffect(isChecked) {
                        if (allCorrect) {
                            onCorrectAnswer()
                            confettiState = listOf(
                                Party(
                                    speed = 0f,
                                    maxSpeed = 30f,
                                    damping = 0.9f,
                                    angle = 270,
                                    spread = 360,
                                    colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xbdb2ff, 0x9bf6ff),
                                    position = Position.Relative(0.5, 0.3),
                                    emitter = Emitter(duration = 100).max(100)
                                )
                            )
                        }
                    }

                    Text(
                        text = if (allCorrect) "כל הכבוד! ✨" else "המילים היו: $hiddenWords",
                        color = if (allCorrect) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { 
                        exoPlayer.seekTo(clips[currentClipIndex].startTimeMs)
                        exoPlayer.play() 
                    }) {
                        Icon(Icons.Default.Replay, null)
                        Text("נגן שוב")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = {
                        if (clips.isNotEmpty()) {
                            if (currentClipIndex < clips.size - 1) {
                                currentClipIndex++
                            } else {
                                currentClipIndex = 0
                            }
                            // Reset quiz state for the next clip
                            userInput = ""
                            isChecked = false
                        }
                    }) {
                        Text("המשפט הבא")
                        Icon(Icons.Default.SkipNext, null)
                    }
                }
            }
        }
    }
}

@Composable
fun XRayDialog(line: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subtitle X-Ray Analysis", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    item {
                        XRaySection("1. ORIGINAL LINE", line)
                        XRaySection("2. NATURAL TRANSLATION", "This is where the human translation goes.")
                        XRaySection("3. WHAT IT ACTUALLY MEANS", "Deep dive into the hidden meaning and subtext.")
                        XRaySection("4. SLANG & EXPRESSIONS", "Slang breakdown and cultural idioms.")
                        XRaySection("5. EMOTIONAL TONE", "The psychological energy of the line.")
                        XRaySection("6. WHY NATIVES SAY IT THIS WAY", "Casual vs Textbook comparison.")
                        XRaySection("7. PRONUNCIATION HINTS", "Reductions and connected speech.")
                        XRaySection("8. CULTURAL CONTEXT", "References and social behavior.")
                        XRaySection("9. SPEAK LIKE THE CHARACTER", "Casual, Confident, and Dramatic versions.")
                        XRaySection("10. QUICK TAKEAWAY", "The one thing to remember forever.")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it!")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun XRaySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp).alpha(0.1f))
    }
}

@Composable
fun VideoListScreen(
    onVideoSelected: (Uri) -> Unit, 
    onPracticeRequested: (File, Boolean) -> Unit,
    onBack: () -> Unit,
    linkedVideos: Set<String>,
    onToggleLink: (String) -> Unit,
    onToggleDifficulty: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val videoDir = remember { File(context.filesDir, "videos") }
    var videoFiles by remember {
        mutableStateOf(videoDir.listFiles()?.filter { it.extension != "srt" } ?: emptyList())
    }
    var videoToRename by remember { mutableStateOf<File?>(null) }
    var videoToDelete by remember { mutableStateOf<File?>(null) }
    var videoToSearchSubtitles by remember { mutableStateOf<File?>(null) }
    var videoToImportSubtitles by remember { mutableStateOf<File?>(null) }
    var videoForQuiz by remember { mutableStateOf<File?>(null) }
    var subtitleSearchQuery by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("קל") }

    if (videoForQuiz != null) {
        AlertDialog(
            onDismissRequest = { videoForQuiz = null },
            title = { Text("בחר רמת קושי לתרגול") },
            text = {
                Column {
                    listOf("קל", "בינוני", "קשה").forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDifficulty = level }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                            Text(text = level, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(
                        text = when(selectedDifficulty) {
                            "קל" -> "מילה אחת חסרה בכל משפט"
                            "בינוני" -> "כ-40% מהמילים יוסתרו"
                            else -> "רוב המילים יוסתרו"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val file = videoForQuiz!!
                    videoForQuiz = null
                    onToggleDifficulty(selectedDifficulty)
                    onPracticeRequested(file, true)
                }) {
                    Text("התחל תרגול")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val file = videoForQuiz!!
                    videoForQuiz = null
                    onPracticeRequested(file, false)
                }) {
                    Text("צפייה רגילה (ללא Quiz)")
                }
            }
        )
    }

    val importSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && videoToImportSubtitles != null) {
            val videoFile = videoToImportSubtitles!!
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    srtFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "כתוביות יובאו בהצלחה!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "שגיאה בייבוא הכתוביות", Toast.LENGTH_SHORT).show()
            }
            videoToImportSubtitles = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Warm Doodle Background ---
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            repeat(10) {
                Row(modifier = Modifier.alpha(0.04f)) {
                    repeat(5) {
                        Icon(Icons.Default.Gesture, null, modifier = Modifier.size(100.dp).padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.AutoFixNormal, null, modifier = Modifier.size(80.dp).padding(10.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("הסרטונים שלי", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (videoFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין סרטונים שמורים. תעלה משהו!", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyColumn {
                    items(videoFiles) { file ->
                        VideoItem(
                            file = file,
                            context = context,
                            onVideoSelected = onVideoSelected,
                            onPractice = { videoForQuiz = file },
                            isLinked = linkedVideos.contains(file.name),
                            onToggleLink = { onToggleLink(file.name) },
                            onRename = { 
                                videoToRename = file
                                newFileName = file.name
                            },
                            onSearchSubtitles = {
                                subtitleSearchQuery = file.nameWithoutExtension
                                videoToSearchSubtitles = file
                            },
                            onImportSubtitles = {
                                videoToImportSubtitles = file
                                importSubtitleLauncher.launch("*/*")
                            },
                            onDelete = { videoToDelete = file }
                        )
                    }
                }
            }
        }
    }

    if (videoToSearchSubtitles != null) {
        AlertDialog(
            onDismissRequest = { videoToSearchSubtitles = null },
            title = { Text("חפש כתוביות להורדה") },
            text = {
                Column {
                    Text("הכנס שם סרט לחיפוש באתרים:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = subtitleSearchQuery,
                        onValueChange = { subtitleSearchQuery = it },
                        placeholder = { Text("למשל: The Matrix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${subtitleSearchQuery}+subtitles+srt"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("חפש ב-Google (כללי)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ktuvit.me/Movie/Search?q=${subtitleSearchQuery}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("חפש ב-Ktuvit (עברית)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { videoToSearchSubtitles = null }) {
                    Text("סגור")
                }
            }
        )
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("מחיקת סרטון") },
            text = { Text("מה ברצונך לעשות עם הסרטון ${videoToDelete?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (videoToDelete?.delete() == true) {
                            videoFiles = videoDir.listFiles()?.filter { it.extension != "srt" } ?: emptyList()
                        }
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("מחק לצמיתות")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        videoToDelete?.let { exportVideoToGallery(context, it) }
                        if (videoToDelete?.delete() == true) {
                            videoFiles = videoDir.listFiles()?.filter { it.extension != "srt" } ?: emptyList()
                        }
                        videoToDelete = null
                        Toast.makeText(context, "הסרטון הועבר לגלריה", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("העבר לגלריה")
                    }
                    TextButton(onClick = { videoToDelete = null }) {
                        Text("ביטול")
                    }
                }
            }
        )
    }

    if (videoToRename != null) {
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("ערוך שם קובץ") },
            text = {
                TextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("שם חדש") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val renamedFile = File(videoToRename!!.parentFile, newFileName)
                    if (videoToRename!!.renameTo(renamedFile)) {
                        videoFiles = videoDir.listFiles()?.filter { it.extension != "srt" } ?: emptyList()
                    }
                    videoToRename = null
                }) {
                    Text("שמור")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun VideoItem(
    file: File,
    context: android.content.Context,
    onVideoSelected: (Uri) -> Unit,
    onPractice: () -> Unit,
    isLinked: Boolean,
    onToggleLink: () -> Unit,
    onRename: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onImportSubtitles: () -> Unit,
    onDelete: () -> Unit
) {
    val duration = remember(file) { getVideoDuration(context, file) }
    val srtFile = remember(file) { File(file.parentFile, "${file.nameWithoutExtension}.srt") }
    val hasSubtitles = srtFile.exists()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onVideoSelected(Uri.fromFile(file)) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayCircle, 
                null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        file.name, 
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isLinked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Linked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = duration ?: "אורך לא ידוע",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (hasSubtitles) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val subLang = remember(srtFile) { detectSubtitleLanguage(srtFile) }
                        Surface(
                            color = if (subLang == "עברית") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = subLang,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (subLang == "עברית") Color(0xFF2E7D32) else Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (hasSubtitles) {
                        DropdownMenuItem(
                            text = { Text("למד ממשפטים (לפי סדר)", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                onPractice()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isLinked) "הסר מהמאגר הרנדומלי" else "הוסף למאגר הרנדומלי") },
                            leadingIcon = { Icon(if (isLinked) Icons.Default.LinkOff else Icons.Default.Link, null) },
                            onClick = {
                                showMenu = false
                                onToggleLink()
                            }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("ערוך שם") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("חפש כתוביות") },
                        leadingIcon = { Icon(Icons.Default.Subtitles, null) },
                        onClick = {
                            showMenu = false
                            onSearchSubtitles()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ייבא כתוביות") },
                        leadingIcon = { Icon(Icons.Default.FileOpen, null) },
                        onClick = {
                            showMenu = false
                            onImportSubtitles()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("מחק", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

fun exportVideoToGallery(context: android.content.Context, file: File) {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LingoFlix")
    }

    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it).use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream!!)
            }
        }
    }
}

fun getVideoDuration(context: android.content.Context, file: File): String? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.fromFile(file))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillis = time?.toLong() ?: 0L
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

fun parseSrtFile(srtFile: File, videoUri: Uri): List<SubtitleClip> {
    val clips = mutableListOf<SubtitleClip>()
    try {
        val bytes = srtFile.readBytes()
        val encoding = detectEncoding(bytes)
        val content = String(bytes, encoding)
        
        // A more robust line-by-line parsing approach
        val lines = content.lines().map { it.trim() }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            // Look for the time line (e.g., 00:00:20,000 --> 00:00:24,400)
            if (line.contains(" --> ")) {
                val times = line.split(" --> ")
                if (times.size == 2) {
                    val startTime = parseSrtTime(times[0])
                    val endTime = parseSrtTime(times[1])
                    
                    // Collect all subsequent non-empty lines as text until the next empty line or index
                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].isNotEmpty() && !lines[i].contains(" --> ")) {
                        // Skip numeric index lines if they appear alone
                        if (lines[i].toIntOrNull() == null) {
                            textLines.add(lines[i])
                        }
                        i++
                    }
                    val text = textLines.joinToString("\n").trim()
                    if (text.isNotEmpty()) {
                        clips.add(SubtitleClip(text, startTime, endTime, videoUri))
                    }
                    continue // i is already incremented
                }
            }
            i++
        }
    } catch (e: Exception) {
        Log.e("SrtParser", "Error parsing SRT", e)
    }
    return clips
}

fun detectEncoding(bytes: ByteArray): java.nio.charset.Charset {
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) return Charsets.UTF_8
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return Charsets.UTF_16BE
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return Charsets.UTF_16LE
    
    // Check for Hebrew (Windows-1255) vs Spanish (ISO-8859-1) vs English (UTF-8)
    var hebrewChars = 0
    var specialSpanishChars = 0
    
    for (b in bytes) {
        val u = b.toInt() and 0xFF
        // Hebrew range in Windows-1255
        if (u in 0xE0..0xFA) hebrewChars++
        // Common Spanish special chars in ISO-8859-1 (ñ, á, é, í, ó, ú, ¿, ¡)
        if (u == 0xF1 || u == 0xD1 || u == 0xE1 || u == 0xE9 || u == 0xED || u == 0xF3 || u == 0xFA || u == 0xBF || u == 0xA1) specialSpanishChars++
    }
    
    return when {
        hebrewChars > 10 -> java.nio.charset.Charset.forName("windows-1255")
        specialSpanishChars > 0 -> Charsets.ISO_8859_1
        else -> Charsets.UTF_8
    }
}

fun parseSrtTime(timeStr: String): Long {
    // Format: 00:00:20,000
    val parts = timeStr.replace(',', '.').split(":")
    if (parts.size != 3) return 0
    val hours = parts[0].toLong()
    val minutes = parts[1].toLong()
    val secondsWithMs = parts[2].toDouble()
    return (hours * 3600000 + minutes * 60000 + (secondsWithMs * 1000).toLong())
}

fun detectSubtitleLanguage(file: File): String {
    return try {
        val content = file.readText(Charsets.UTF_8)
        val hasHebrew = content.any { it in '\u0590'..'\u05FF' }
        if (hasHebrew) "עברית" else "אנגלית"
    } catch (e: Exception) {
        try {
            val content = file.readText(java.nio.charset.Charset.forName("windows-1255"))
            val hasHebrew = content.any { it in '\u0590'..'\u05FF' }
            if (hasHebrew) "עברית" else "אנגלית"
        } catch (e2: Exception) {
            "לא ידוע"
        }
    }
}
