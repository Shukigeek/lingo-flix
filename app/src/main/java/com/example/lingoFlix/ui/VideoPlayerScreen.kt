package com.example.lingoFlix.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.lingoFlix.ui.components.*
import com.example.lingoFlix.utils.SrtParser
import com.example.lingoFlix.utils.SubtitleGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    isRandomMode: Boolean = false,
    difficulty: String = "קל",
    onCorrectAnswer: () -> Unit = {},
    userId: String = "guest"
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isFullScreen by remember { mutableStateOf(false) }
    var currentClipIndex by remember { mutableIntStateOf(0) }
    
    // Progress Saving and Resuming
    val videoFileName = remember(videoUri) {
        try {
            if (videoUri.scheme == "file") {
                File(videoUri.path!!).name
            } else if (videoUri.scheme == "content") {
                videoUri.lastPathSegment ?: videoUri.toString().hashCode().toString()
            } else {
                videoUri.toString().hashCode().toString()
            }
        } catch (e: Exception) {
            "unknown"
        }
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
    var subtitlesVisible by rememberSaveable { mutableStateOf(true) }
    
    // Improvement 6: Session states
    var isSessionComplete by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalAttempted by remember { mutableIntStateOf(0) }

    var hiddenIndices by remember(currentClipIndex) { mutableStateOf(setOf<Int>()) }
    var wordsList by remember(currentClipIndex) { mutableStateOf(listOf<String>()) }
    
    // Confetti State
    var confettiState by remember { mutableStateOf<List<Party>>(emptyList()) }
    
    // X-Ray State
    var showXRay by remember { mutableStateOf(false) }
    var xRayLine by remember { mutableStateOf("") }
    
    // Voice Input State
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        activityResult.data?.let { data ->
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                userInput = results[0]
            }
        }
    }
    
    // Subtitle Generation State
    var isGeneratingSubtitles by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf("") }
    var subtitlesGeneratedTrigger by remember { mutableIntStateOf(0) }
    
    var showNoSubtitlesMessage by remember { mutableStateOf(false) }
    
    // Detected language for the video
    val detectedLanguage = remember(videoUri, subtitlesGeneratedTrigger) {
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
    val allClipsForThisVideo = remember(videoUri, subtitlesGeneratedTrigger) {
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
            
            val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
            if (currentUri != clip.videoUri) {
                val mediaItemBuilder = MediaItem.Builder().setUri(clip.videoUri)
                exoPlayer.setMediaItem(mediaItemBuilder.build())
                exoPlayer.prepare()
                // Wait a bit for preparation if URI changed
                delay(200)
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
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

        if (isGeneratingSubtitles) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "מייצר כתוביות בעזרת AI...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = generationProgress,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { /* הוספת פתיחת הגדרות אם נדרש */ },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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

                IconButton(
                    onClick = { subtitlesVisible = subtitlesVisible.not() },
                    modifier = Modifier.background(
                        if (subtitlesVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    )
                ) {
                    Icon(
                        imageVector = if (subtitlesVisible) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                        contentDescription = "Toggle Subtitles",
                        tint = Color.White
                    )
                }
            }

            if (clips == null && allClipsForThisVideo.isEmpty()) {
                LaunchedEffect(Unit) {
                    showNoSubtitlesMessage = true
                    delay(5000)
                    showNoSubtitlesMessage = false
                }
            }

            if (showNoSubtitlesMessage) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = "אין כתוביות לסרטון הזה",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (clips != null && subtitlesVisible) {
                if (!isRandomMode) {
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
                }

                Spacer(modifier = Modifier.weight(1f))
                
                val currentClip = clips[currentClipIndex]
                val currentClipVideoName = remember(currentClip.videoUri) {
                    if (currentClip.videoUri.scheme == "file") {
                        File(currentClip.videoUri.path!!).name
                    } else {
                        currentClip.videoUri.lastPathSegment ?: currentClip.videoUri.toString().hashCode().toString()
                    }
                }
                val clipId = "$currentClipVideoName|${currentClip.startTimeMs}"
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            placeholder = { Text("הקלד את המילים החסרות...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isChecked = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Check")
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferredAudioLang)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "דבר עכשיו...")
                                    // Try to prefer offline recognition if available
                                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                }
                                try {
                                    voiceLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "זיהוי קולי לא זמין", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color.White)
                        }
                    }
                } else if (isQuizMode && isChecked) {
                    val hiddenWords = wordsList.filterIndexed { index, _ -> hiddenIndices.contains(index) }.joinToString(" ")
                    val allCorrect = hiddenIndices.all { idx ->
                        userInput.split(Regex("\\s+")).any { it.trim().equals(wordsList[idx].trim(), ignoreCase = true) }
                    }
                    
                    LaunchedEffect(isChecked) {
                        if (allCorrect) {
                            correctCount++
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
                            totalAttempted++
                            if (currentClipIndex < clips.size - 1) {
                                currentClipIndex++
                            } else {
                                isSessionComplete = true
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

        // Improvement 6: Completion Screen Overlay
        AnimatedVisibility(
            visible = isSessionComplete,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎬 סיימת את האימון!",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "ענית נכון על $correctCount מתוך $totalAttempted משפטים",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    val sessionXP = correctCount * when(difficulty) {
                        "בינוני" -> 30
                        "קשה" -> 40
                        else -> 20
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "+$sessionXP XP נצברו!",
                        color = Color(0xFFFFC107),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    DuoButton(
                        text = "שחק שוב",
                        onClick = {
                            isSessionComplete = false
                            correctCount = 0
                            totalAttempted = 0
                            currentClipIndex = 0
                            userInput = ""
                            isChecked = false
                        },
                        color = DuoGreen,
                        darkColor = DuoDarkGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DuoButton(
                        text = "חזור לבית",
                        onClick = onBack,
                        color = DuoBlue,
                        darkColor = DuoBlue.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            LaunchedEffect(isSessionComplete) {
                if (isSessionComplete) {
                    confettiState = listOf(
                        Party(
                            speed = 0f,
                            maxSpeed = 30f,
                            damping = 0.9f,
                            angle = 270,
                            spread = 360,
                            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xbdb2ff, 0x9bf6ff),
                            position = Position.Relative(0.5, 0.3),
                            emitter = Emitter(duration = 200).max(200)
                        )
                    )
                }
            }
        }
    }
}
