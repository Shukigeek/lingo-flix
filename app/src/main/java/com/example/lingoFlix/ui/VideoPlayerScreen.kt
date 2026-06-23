package com.example.lingoFlix.ui

import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
import com.example.lingoFlix.utils.*
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
    onCorrectAnswer: (Int) -> Unit = {},
    userId: String = "guest",
    quizType: String = "typing"
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isFullScreen by remember { mutableStateOf(false) }
    var currentClipIndex by remember { mutableIntStateOf(0) }
    
    // Progress Saving and Resuming
    val currentClip = remember(clips, currentClipIndex) {
        if (clips != null && currentClipIndex < clips.size) clips[currentClipIndex] else null
    }
    
    val videoFileName = remember(currentClip?.videoUri ?: videoUri) {
        val targetUri = currentClip?.videoUri ?: videoUri
        try {
            if (targetUri.scheme == "file") {
                File(targetUri.path!!).name
            } else if (targetUri.scheme == "content") {
                targetUri.lastPathSegment ?: targetUri.toString().hashCode().toString()
            } else {
                targetUri.toString().hashCode().toString()
            }
        } catch (e: Exception) {
            LingoLog.e("VideoPlayerScreen", "Error getting video filename", e)
            "unknown"
        }
    }
    var showResumeDialog by remember { mutableStateOf(false) }
    var savedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(videoFileName, clips) {
        if (clips != null && videoFileName != "unknown" && !isRandomMode) {
            try {
                val prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
                val progress = prefs.getInt("progress_$videoFileName", -1)
                if (progress > 0 && progress < clips.size) {
                    savedIndex = progress
                    showResumeDialog = true
                }
            } catch (e: Exception) {
                LingoLog.e("VideoPlayerScreen", "Error loading progress", e)
            }
        }
    }

    LaunchedEffect(currentClipIndex) {
        if (clips != null && videoFileName != "unknown" && !isRandomMode) {
            try {
                context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("progress_$videoFileName", currentClipIndex)
                    .apply()
            } catch (e: Exception) {
                LingoLog.e("VideoPlayerScreen", "Error saving progress", e)
            }
        }
    }

    if (showResumeDialog) {
        ResumeDialog(
            savedIndex = savedIndex,
            onConfirm = {
                currentClipIndex = savedIndex
                showResumeDialog = false
            },
            onDismiss = { showResumeDialog = false }
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
    var comboCount by remember { mutableIntStateOf(0) }
    var heartsLeft by remember { mutableIntStateOf(10) }

    var hiddenIndices by remember(currentClipIndex) { mutableStateOf(setOf<Int>()) }
    var wordsList by remember(currentClipIndex) { mutableStateOf(listOf<String>()) }
    
    // Confetti State
    var confettiState by remember { mutableStateOf<List<Party>>(emptyList()) }
    
    // X-Ray State
    var showXRay by remember { mutableStateOf(false) }
    var xRayLine by remember { mutableStateOf("") }
    var xRayMissingWords by remember { mutableStateOf("") }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSyncTest by remember { mutableStateOf(false) }
    
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
    
    var showInfoMessage by remember { mutableStateOf<String?>(null) }
    
    var showSearchDialog by remember { mutableStateOf(false) }
    var subtitleSearchQuery by remember { mutableStateOf("") }
    
    val importSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && videoUri.scheme == "file") {
            val videoFile = File(videoUri.path!!)
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    srtFile.outputStream().use { output -> input.copyTo(output) }
                }
                Toast.makeText(context, "כתוביות יובאו! טוען מחדש...", Toast.LENGTH_SHORT).show()
                subtitlesGeneratedTrigger++
            } catch (e: Exception) {
                Toast.makeText(context, "שגיאה בייבוא", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Subtitle Styling states
    val sharedPrefs = remember { context.getSharedPreferences("lingo_prefs", Context.MODE_PRIVATE) }
    var subtitleFontSize by remember { mutableFloatStateOf(sharedPrefs.getFloat("sub_font_size", 34f)) }
    var subtitleColorHex by remember { mutableStateOf(sharedPrefs.getString("sub_color", "#FFFFFF") ?: "#FFFFFF") }
    var subtitleIsBold by remember { mutableStateOf(sharedPrefs.getBoolean("sub_is_bold", true)) }
    var subtitleFontFamily by remember { mutableStateOf(sharedPrefs.getString("sub_font_family", "SansSerif") ?: "SansSerif") }
    var showStyleDialog by remember { mutableStateOf(false) }

    // Multiple Choice States removed as requested

    LaunchedEffect(currentClipIndex, quizType, clips) {
        // Multiple Choice Logic removed
    }

    // Shake and Flash Animation States
    val shakeOffset = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    
    // Floating XP State
    var floatingXP by remember { mutableStateOf<Int?>(null) }
    val floatingXPAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        SoundManager.init(context)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // We don't release SoundManager here if it's a singleton used elsewhere,
            // but for now let's assume it's okay to keep it alive or release it.
        }
    }

    val currentFontFamily = when(subtitleFontFamily) {
        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
        "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }

    // Detected language for the video/clip
    val detectedLanguage = remember(videoUri, clips, currentClipIndex, subtitlesGeneratedTrigger) {
        // Priority 1: Check the current clip text if it's Hebrew
        val currentText = if (clips != null && currentClipIndex < clips.size) clips[currentClipIndex].text else ""
        if (currentText.any { it in '\u0590'..'\u05FF' }) return@remember "עברית"
        
        // Priority 2: Check the SRT file
        val targetUri = if (clips != null && currentClipIndex < clips.size) clips[currentClipIndex].videoUri else videoUri
        if (targetUri.scheme == "file") {
            val videoFile = File(targetUri.path!!)
            val srtFile = FileUtils.findBestSrtForVideo(videoFile)
            if (srtFile != null && srtFile.exists()) {
                val lang = SrtParser.detectSubtitleLanguage(srtFile)
                Log.d("VideoPlayerScreen", "Detected language for ${videoFile.name}: $lang")
                lang
            } else "אנגלית"
        } else "אנגלית"
    }

    val preferredAudioLang = when (detectedLanguage) {
        "עברית" -> "he"
        "ספרדית" -> "es"
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
        if (isQuizMode && clips != null && currentClipIndex < clips.size) {
            val originalText = clips[currentClipIndex].text
            // Split by spaces and punctuation
            val words = originalText.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;])|(?=[.,!?;])")).filter { it.isNotBlank() }
            wordsList = words
            
            val validIndices = words.indices.filter { words[it].length > 1 && words[it].any { c -> c.isLetter() } }
            
            if (validIndices.isNotEmpty()) {
                val countToHide = if (quizType == "multiple_choice") 1 else {
                    when (difficulty) {
                        "בינוני" -> (validIndices.size * 0.4).toInt().coerceAtLeast(1)
                        "קשה" -> (validIndices.size * 0.7).toInt().coerceAtLeast(1)
                        else -> 1 // קל
                    }
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
            val srtFile = FileUtils.findBestSrtForVideo(videoFile)
            if (srtFile != null && srtFile.exists()) SrtParser.parseSrtFile(srtFile, videoUri) else emptyList()
        } else emptyList()
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            playWhenReady = true
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("VideoPlayerScreen", "ExoPlayer Error: ${error.message}", error)
                }
                override fun onPlaybackStateChanged(state: Int) {
                    val stateStr = when(state) {
                        androidx.media3.common.Player.STATE_IDLE -> "IDLE"
                        androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
                        androidx.media3.common.Player.STATE_READY -> "READY"
                        androidx.media3.common.Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN"
                    }
                    Log.d("VideoPlayerScreen", "Playback State Changed: $stateStr")
                }
            })
        }
    }
    
    // Handle Speed Change
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Handle Video Change and Seeking
    LaunchedEffect(videoUri, clips, currentClipIndex) {
        val targetUri = if (clips != null && currentClipIndex < clips.size) {
            clips[currentClipIndex].videoUri
        } else {
            videoUri
        }
        
        val currentMediaUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
        
        val isNewVideo = currentMediaUri?.toString() != targetUri.toString()

        if (isNewVideo) {
            Log.d("VideoPlayerScreen", "Loading new media: $targetUri")
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            
            val mediaItemBuilder = MediaItem.Builder().setUri(targetUri)
            
            // Add subtitles if in regular mode and available
            if (clips == null && targetUri.scheme == "file") {
                val videoFile = File(targetUri.path!!)
                val srtFile = FileUtils.findBestSrtForVideo(videoFile)
                if (srtFile != null && srtFile.exists()) {
                    val utf8Srt = FileUtils.getUtf8SrtFile(context, srtFile)
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(utf8Srt))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage(if (detectedLanguage == "עברית") "he" else "en")
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
                }
            }

            // Calculate start position
            val startPos = if (clips != null && currentClipIndex < clips.size) {
                val seekBack = 200L // Reduced from 600ms for more precision
                (clips[currentClipIndex].startTimeMs - seekBack).coerceAtLeast(0L)
            } else 0L

            exoPlayer.setMediaItem(mediaItemBuilder.build(), startPos)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false // Wait for seek then play
            
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage(preferredAudioLang)
                .build()

            // Bug #5: Wait until player is ready before seeking
            var waited = 0
            while (exoPlayer.playbackState != androidx.media3.common.Player.STATE_READY && waited < 5000) {
                delay(50)
                waited += 50
            }
        } else if (clips != null && currentClipIndex < clips.size) {
            // Same video, but check if we need to seek to a different clip
            val currentPos = exoPlayer.currentPosition
            val clipStart = clips[currentClipIndex].startTimeMs
            val seekBack = 200L
            val targetPos = (clipStart - seekBack).coerceAtLeast(0L)
            
            if (Math.abs(currentPos - targetPos) > 500) { 
                Log.d("VideoPlayerScreen", "Seeking to clip start: $targetPos")
                exoPlayer.seekTo(targetPos)
            }
        }

        exoPlayer.play()
    }

    // Check if clip ended
    if (clips != null) {
        LaunchedEffect(currentClipIndex, clips) {
            while (true) {
                delay(30) // More frequent checks (30ms instead of 100ms)
                if (currentClipIndex < clips.size) {
                    val clip = clips[currentClipIndex]
                    val endBuffer = 300L // Reduced from 1200ms/600ms for tighter timing
                    if (exoPlayer.currentPosition >= clip.endTimeMs + endBuffer) {
                        exoPlayer.pause()
                        break
                    }
                } else break
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
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            if (isFullScreen) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                // Respect immersive mode: hide navigation, show status
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.show(WindowInsetsCompat.Type.statusBars())
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
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    if (showStyleDialog) {
        SubtitleStyleDialog(
            fontSize = subtitleFontSize,
            onFontSizeChange = { subtitleFontSize = it },
            fontFamily = subtitleFontFamily,
            onFontFamilyChange = { subtitleFontFamily = it },
            isBold = subtitleIsBold,
            onBoldChange = { subtitleIsBold = it },
            colorHex = subtitleColorHex,
            onColorChange = { subtitleColorHex = it },
            onSave = {
                try {
                    sharedPrefs.edit()
                        .putFloat("sub_font_size", subtitleFontSize)
                        .putString("sub_color", subtitleColorHex)
                        .putBoolean("sub_is_bold", subtitleIsBold)
                        .putString("sub_font_family", subtitleFontFamily)
                        .apply()
                    showStyleDialog = false
                } catch (e: Exception) {
                    LingoLog.e("VideoPlayerScreen", "Error saving subtitle styles", e)
                }
            },
            onDismiss = { showStyleDialog = false }
        )
    }

    if (showXRay) {
        XRayDialog(
            line = xRayLine,
            missingWords = xRayMissingWords,
            sourceLang = detectedLanguage,
            onDismiss = { showXRay = false },
            userId = userId
        )
    }

    if (showSearchDialog) {
        SubtitleSearchDialog(
            videoUri = videoUri,
            searchQuery = subtitleSearchQuery,
            onSearchQueryChange = { subtitleSearchQuery = it },
            onGoogleSearch = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${subtitleSearchQuery}+subtitles+srt"))) },
            onKtuvitSearch = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ktuvit.me/Movie/Search?q=${subtitleSearchQuery}"))) },
            onImportFile = { importSubtitleLauncher.launch("*/*") },
            onGenerateAI = {
                scope.launch {
                    try {
                        showSearchDialog = false
                        isGeneratingSubtitles = true
                        val videoFile = File(videoUri.path!!)
                        val result = SubtitleGenerator.generateSubtitles(context, videoFile, userId) { generationProgress = it }
                        isGeneratingSubtitles = false
                        result.onSuccess {
                            Toast.makeText(context, "כתוביות נוצרו בהצלחה!", Toast.LENGTH_SHORT).show()
                            subtitlesGeneratedTrigger++
                        }.onFailure {
                            LingoLog.e("VideoPlayerScreen", "AI Subtitle generation failed", it)
                            Toast.makeText(context, "שגיאה ביצירה: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        LingoLog.e("VideoPlayerScreen", "Exception during subtitle generation", e)
                        isGeneratingSubtitles = false
                    }
                }
            },
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showSyncTest) {
        SyncTestDialog(
            onSeek = { percentage ->
                try {
                    exoPlayer.seekTo((exoPlayer.duration * percentage).toLong())
                    exoPlayer.play()
                } catch (e: Exception) {
                    LingoLog.e("VideoPlayerScreen", "Seek error during sync test", e)
                }
            },
            onDismiss = { showSyncTest = false }
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
            GenerationOverlay(progress = generationProgress)
        }

        // HUD: Hearts, Combo, Auto-Advance
        if (isQuizMode && !isSessionComplete) {
            QuizHUD(heartsLeft = heartsLeft, comboCount = comboCount)
        }
        
        // Custom HUD (Top Controls)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
            TopControls(
                isFullScreen = isFullScreen,
                onFullScreenToggle = { isFullScreen = !isFullScreen },
                playbackSpeed = playbackSpeed,
                onPlaybackSpeedToggle = { playbackSpeed = if (playbackSpeed == 1.0f) 0.7f else 1.0f },
                onStyleClick = { showStyleDialog = true },
                onSearchClick = { showSearchDialog = true },
                onBack = onBack
            )

            if (clips == null && allClipsForThisVideo.isEmpty()) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "אין כתוביות לסרטון הזה", Toast.LENGTH_SHORT).show()
                    delay(5000)
                    showInfoMessage = null
                }
            }

            if (showInfoMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = showInfoMessage!!,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (clips != null && subtitlesVisible && currentClipIndex < clips.size) {
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

                SubtitleSection(
                    text = currentClip.text,
                    isQuizMode = isQuizMode,
                    hiddenIndices = hiddenIndices,
                    wordsList = wordsList,
                    userInput = userInput,
                    isChecked = isChecked,
                    detectedLanguage = detectedLanguage,
                    subtitleColorHex = subtitleColorHex,
                    subtitleFontSize = subtitleFontSize,
                    subtitleIsBold = subtitleIsBold,
                    currentFontFamily = currentFontFamily,
                    shakeOffset = shakeOffset.value,
                    flashColor = flashColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(clipId) },
                        modifier = Modifier
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
                            xRayMissingWords = if (isQuizMode) {
                                hiddenIndices.map { wordsList[it] }.joinToString(" ")
                            } else ""
                            showXRay = true 
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = "Subtitle X-Ray",
                            tint = Color.White
                        )
                    }
                }

                if (isQuizMode) {
                    val hiddenWords = wordsList.filterIndexed { index, _ -> hiddenIndices.contains(index) }.joinToString(" ")
                    val allCorrect = isChecked && hiddenIndices.all { idx ->
                        userInput.split(Regex("\\s+")).any { it.trim().equals(wordsList[idx].trim(), ignoreCase = true) }
                    }
                    
                    QuizInputSection(
                        userInput = userInput,
                        onUserInputChange = { userInput = it },
                        onCheck = { isChecked = true },
                        isChecked = isChecked,
                        allCorrect = allCorrect,
                        hiddenWords = hiddenWords
                    )

                    LaunchedEffect(isChecked) {
                        if (isChecked) {
                            if (allCorrect) {
                                correctCount++
                                comboCount++
                                val multiplier = when {
                                    comboCount >= 10 -> 10
                                    comboCount >= 5 -> 5
                                    comboCount >= 3 -> 3
                                    comboCount >= 2 -> 2
                                    else -> 1
                                }
                                onCorrectAnswer(multiplier)
                                flashColor = Color.Green
                                delay(500)
                                flashColor = Color.Transparent
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
                            } else {
                                comboCount = 0
                                heartsLeft = (heartsLeft - 1).coerceAtLeast(0)
                                flashColor = Color.Red
                                repeat(3) {
                                    shakeOffset.animateTo(10f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
                                    shakeOffset.animateTo(-10f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
                                }
                                shakeOffset.animateTo(0f)
                                delay(500)
                                flashColor = Color.Transparent
                            }
                        }
                    }
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
            SessionCompleteOverlay(
                correctCount = correctCount,
                totalAttempted = totalAttempted,
                difficulty = difficulty,
                onPlayAgain = {
                    isSessionComplete = false
                    correctCount = 0
                    totalAttempted = 0
                    currentClipIndex = 0
                    userInput = ""
                    isChecked = false
                },
                onBack = onBack
            )
            
            LaunchedEffect(isSessionComplete) {
                if (isSessionComplete) {
                    try {
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
                    } catch (e: Exception) {
                        LingoLog.e("VideoPlayerScreen", "Error showing confetti", e)
                    }
                }
            }
        }
        // Game Over Dialog
        if (isQuizMode && heartsLeft == 0 && !isSessionComplete) {
            GameOverDialog(
                completedCount = currentClipIndex,
                onRetry = {
                    heartsLeft = 10
                    currentClipIndex = 0
                    userInput = ""
                    isChecked = false
                    comboCount = 0
                },
                onBack = onBack
            )
        }
    }
}
