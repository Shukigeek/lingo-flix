package com.example.lingoFlix.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.util.SoundManager
import com.example.lingoFlix.utils.*
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onSaveWord: (String, String) -> Unit = { _, _ -> },
    userId: String = "guest",
    quizType: String = "typing"
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isFullScreen by remember { mutableStateOf(false) }
    var currentClipIndex by remember { mutableIntStateOf(0) }
    
    var selectedWordForDialog by remember { mutableStateOf<String?>(null) }

    val currentClip = remember(clips, currentClipIndex) {
        if (clips != null && currentClipIndex < clips.size) clips[currentClipIndex] else null
    }
    
    val videoFileName = remember(currentClip?.videoUri ?: videoUri) {
        val targetUri = currentClip?.videoUri ?: videoUri
        try {
            if (targetUri.scheme == "file") File(targetUri.path!!).name
            else if (targetUri.scheme == "content") targetUri.lastPathSegment ?: targetUri.toString().hashCode().toString()
            else targetUri.toString().hashCode().toString()
        } catch (e: Exception) { "unknown" }
    }
    var showResumeDialog by remember { mutableStateOf(false) }
    var savedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(videoFileName, clips) {
        if (clips != null && videoFileName != "unknown" && !isRandomMode) {
            val prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
            val progress = prefs.getInt("progress_$videoFileName", -1)
            if (progress > 0 && progress < clips.size) { savedIndex = progress; showResumeDialog = true }
        }
    }

    LaunchedEffect(currentClipIndex) {
        if (clips != null && videoFileName != "unknown" && !isRandomMode) {
            context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE).edit().putInt("progress_$videoFileName", currentClipIndex).apply()
        }
    }

    if (showResumeDialog) ResumeDialog(savedIndex = savedIndex, onConfirm = { currentClipIndex = savedIndex; showResumeDialog = false }, onDismiss = { showResumeDialog = false })
    
    var userInput by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }
    var subtitlesVisible by rememberSaveable { mutableStateOf(true) }
    var isSessionComplete by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalAttempted by remember { mutableIntStateOf(0) }
    var comboCount by remember { mutableIntStateOf(0) }
    var heartsLeft by remember { mutableIntStateOf(10) }

    var hiddenIndices by remember(currentClipIndex) { mutableStateOf(setOf<Int>()) }
    var wordsList by remember(currentClipIndex) { mutableStateOf(listOf<String>()) }
    var confettiState by remember { mutableStateOf<List<Party>>(emptyList()) }
    var showXRay by remember { mutableStateOf(false) }
    var xRayLine by remember { mutableStateOf("") }
    var xRayMissingWords by remember { mutableStateOf("") }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var showSyncTest by remember { mutableStateOf(false) }
    var isGeneratingSubtitles by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf("") }
    var subtitlesGeneratedTrigger by remember { mutableIntStateOf(0) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var subtitleSearchQuery by remember { mutableStateOf("") }
    
    val sharedPrefs = remember { context.getSharedPreferences("lingo_prefs", Context.MODE_PRIVATE) }
    
    LaunchedEffect(videoUri) {
        LingoLog.d("VideoPlayerScreen", "Initializing for video: $videoUri")
    }

    var subtitleFontSize by remember { mutableFloatStateOf(sharedPrefs.getFloat("sub_font_size", 34f)) }
    var subtitleColorHex by remember { mutableStateOf(sharedPrefs.getString("sub_color", "#FFFFFF") ?: "#FFFFFF") }
    var subtitleIsBold by remember { mutableStateOf(sharedPrefs.getBoolean("sub_is_bold", true)) }
    var subtitleFontFamily by remember { mutableStateOf(sharedPrefs.getString("sub_font_family", "SansSerif") ?: "SansSerif") }
    var showStyleDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    
    LaunchedEffect(Unit) { SoundManager.init(context) }
    
    val currentFontFamily = VideoPlayerLogic.getFontFamily(subtitleFontFamily)
    val detectedLanguage = remember(videoUri, clips, currentClipIndex, subtitlesGeneratedTrigger) { VideoPlayerLogic.detectLanguage(videoUri, clips, currentClipIndex) }
    val preferredAudioLang = VideoPlayerLogic.getPreferredAudioLang(detectedLanguage)

    LaunchedEffect(currentClipIndex, clips, isQuizMode) {
        if (isQuizMode && clips != null && currentClipIndex < clips.size) {
            val (indices, words) = VideoPlayerLogic.prepareQuiz(clips[currentClipIndex].text, difficulty, quizType)
            wordsList = words; hiddenIndices = indices; userInput = ""; isChecked = false
        }
    }
    
    val exoPlayer = remember(context) { ExoPlayer.Builder(context).build().apply { setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT); playWhenReady = true } }
    
    VideoPlayerController(exoPlayer, playbackSpeed, videoUri, clips, currentClipIndex, detectedLanguage, preferredAudioLang, lifecycleOwner, activity, isFullScreen, context, isQuizMode)

    val allCorrect = isChecked && hiddenIndices.all { idx -> userInput.split(Regex("\\s+")).any { it.trim().equals(wordsList[idx].trim(), ignoreCase = true) } }

    VideoPlayerDialogsOverlay(
        showStyleDialog, { showStyleDialog = false }, subtitleFontSize, { subtitleFontSize = it }, subtitleFontFamily, { subtitleFontFamily = it }, subtitleIsBold, { subtitleIsBold = it }, subtitleColorHex, { subtitleColorHex = it }, 
        { sharedPrefs.edit().putFloat("sub_font_size", subtitleFontSize).putString("sub_color", subtitleColorHex).putBoolean("sub_is_bold", subtitleIsBold).putString("sub_font_family", subtitleFontFamily).apply(); showStyleDialog = false },
        showXRay, { showXRay = false }, xRayLine, xRayMissingWords, detectedLanguage, userId, showSearchDialog, { showSearchDialog = false }, videoUri, subtitleSearchQuery, { subtitleSearchQuery = it }, { subtitlesGeneratedTrigger++ }, { isGeneratingSubtitles = it }, { generationProgress = it }, showSyncTest, { showSyncTest = false }, exoPlayer
    )

    VideoPlayerScreenUI(
        exoPlayer, confettiState, isGeneratingSubtitles, generationProgress, isQuizMode, isSessionComplete, heartsLeft, comboCount, isFullScreen, { isFullScreen = !isFullScreen }, playbackSpeed, { playbackSpeed = if (playbackSpeed == 1.0f) 0.7f else 1.0f }, isPlaying, { isPlaying = !isPlaying; if (isPlaying) exoPlayer.play() else exoPlayer.pause() }, { showStyleDialog = true }, { showSearchDialog = true }, onBack,
        clips, subtitlesVisible, currentClipIndex, favoriteClips, onToggleFavorite, hiddenIndices, wordsList, userInput, { userInput = it }, isChecked, { isChecked = true }, detectedLanguage, subtitleColorHex, subtitleFontSize, subtitleIsBold, currentFontFamily, shakeOffset.value, flashColor, correctCount, totalAttempted, difficulty,
        { isSessionComplete = false; correctCount = 0; totalAttempted = 0; currentClipIndex = 0; userInput = ""; isChecked = false }, { exoPlayer.seekTo(clips!![currentClipIndex].startTimeMs); exoPlayer.play() },
        { if (clips != null && clips.isNotEmpty()) { totalAttempted++; if (currentClipIndex < clips.size - 1) currentClipIndex++ else isSessionComplete = true; userInput = ""; isChecked = false } },
        { line, missing -> xRayLine = line; xRayMissingWords = missing; showXRay = true },
        onWordClick = { selectedWordForDialog = it }
    )

    if (selectedWordForDialog != null) {
        com.example.lingoFlix.ui.components.WordDetailsDialog(
            word = selectedWordForDialog!!,
            sourceLang = detectedLanguage,
            onDismiss = { selectedWordForDialog = null },
            onAddToLearning = { word, trans -> 
                onSaveWord(word, trans)
            }
        )
    }

    LaunchedEffect(isChecked) {
        if (isChecked && isQuizMode) {
            if (allCorrect) {
                correctCount++; comboCount++; onCorrectAnswer(if (comboCount >= 10) 10 else if (comboCount >= 5) 5 else if (comboCount >= 3) 3 else if (comboCount >= 2) 2 else 1)
                flashColor = Color.Green; delay(500); flashColor = Color.Transparent
                confettiState = listOf(Party(speed = 0f, maxSpeed = 30f, damping = 0.9f, angle = 270, spread = 360, colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xbdb2ff, 0x9bf6ff), position = Position.Relative(0.5, 0.3), emitter = Emitter(duration = 100).max(100)))
            } else {
                comboCount = 0; heartsLeft = (heartsLeft - 1).coerceAtLeast(0); flashColor = Color.Red
                repeat(3) { shakeOffset.animateTo(10f, spring(Spring.DampingRatioHighBouncy)); shakeOffset.animateTo(-10f, spring(Spring.DampingRatioHighBouncy)) }
                shakeOffset.animateTo(0f); delay(500); flashColor = Color.Transparent
            }
        }
    }

    LaunchedEffect(isSessionComplete) {
        if (isSessionComplete) confettiState = listOf(Party(speed = 0f, maxSpeed = 30f, damping = 0.9f, angle = 270, spread = 360, colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xbdb2ff, 0x9bf6ff), position = Position.Relative(0.5, 0.3), emitter = Emitter(duration = 200).max(200)))
    }

    if (isQuizMode && heartsLeft == 0 && !isSessionComplete) GameOverDialog(completedCount = currentClipIndex, onRetry = { heartsLeft = 10; currentClipIndex = 0; userInput = ""; isChecked = false; comboCount = 0 }, onBack = onBack)
}
