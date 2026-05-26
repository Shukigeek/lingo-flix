package com.example.lingoFlix.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.ui.components.XRayDialog
import com.example.lingoFlix.utils.SrtParser
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.io.File

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
    onCorrectAnswer: () -> Unit = {},
    userId: String = "guest"
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullScreen by remember { mutableStateOf(false) }
    var currentClipIndex by remember { mutableIntStateOf(0) }
    
    // Progress Saving and Resuming
    val videoFileName = remember(videoUri) {
        if (videoUri.scheme == "file") File(videoUri.path!!).name else "unknown"
    }
    var showResumeDialog by remember { mutableStateOf(false) }
    var savedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(videoFileName, clips) {
        if (clips != null && videoFileName != "unknown") {
            val prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
            val progress = prefs.getInt("progress_$videoFileName", -1)
            if (progress > 0 && progress < clips.size) {
                savedIndex = progress
                showResumeDialog = true
            }
        }
    }

    LaunchedEffect(currentClipIndex) {
        if (clips != null && videoFileName != "unknown") {
            context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
                .edit()
                .putInt("progress_$videoFileName", currentClipIndex)
                .apply()
        }
    }

    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("המשך תרגול") },
            text = { Text("נראה שהיית באמצע התרגול. האם להמשיך ממשפט ${savedIndex + 1}?") },
            confirmButton = {
                Button(onClick = {
                    currentClipIndex = savedIndex
                    showResumeDialog = false
                }) {
                    Text("המשך")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResumeDialog = false }) {
                    Text("התחל מהתחלה")
                }
            }
        )
    }
    
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
    
    // Detected language for the video
    val detectedLanguage = remember(videoUri) {
        if (videoUri.scheme == "file") {
            val videoFile = File(videoUri.path!!)
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            if (srtFile.exists()) SrtParser.detectSubtitleLanguage(srtFile) else "אנגלית"
        } else "אנגלית"
    }

    val preferredAudioLang = when (detectedLanguage) {
        "עברית" -> "he"
        "ספרדית" -> "es"
        "רוסית" -> "ru"
        "ערבית" -> "ar"
        "צרפתית" -> "fr"
        "גרמנית" -> "de"
        "איטלקית" -> "it"
        "פורטוגזית" -> "pt"
        "סינית" -> "zh"
        "יפנית" -> "ja"
        "קוריאנית" -> "ko"
        else -> "en"
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
            if (srtFile.exists()) SrtParser.parseSrtFile(srtFile, videoUri) else emptyList()
        } else emptyList()
    }

    val exoPlayer = remember(videoUri) {
        val videoFile = if (videoUri.scheme == "file") File(videoUri.path!!) else null
        val srtFile = videoFile?.let { File(it.parentFile, "${it.nameWithoutExtension}.srt") }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUri)

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
                .setPreferredAudioLanguage(preferredAudioLang)
                .build()
        }
    }
    
    // Track current position for favoriting in regular mode
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    
    if (clips == null && allClipsForThisVideo.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                currentPlaybackPosition = exoPlayer.currentPosition
                delay(500)
            }
        }
    }

    // Logic for playing specific clips
    LaunchedEffect(clips, currentClipIndex) {
        clips?.let {
            val clip = it[currentClipIndex]
            
            if (exoPlayer.currentMediaItem?.localConfiguration?.uri != clip.videoUri) {
                val mediaItemBuilder = MediaItem.Builder().setUri(clip.videoUri)
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
                delay(100)
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
            onDismiss = { showXRay = false },
            userId = userId
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = clips == null
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
                    val isRtl = detectedLanguage == "עברית"
                    
                    Column(modifier = Modifier.padding(20.dp)) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                        ) {
                            if (isQuizMode && hiddenIndices.isNotEmpty()) {
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
