package com.example.lingoFlix.ui

import android.content.Context
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.util.GameLogic
import com.example.lingoFlix.util.SoundManager
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
    difficulty: String = "קל",
    onCorrectAnswer: (Int) -> Unit = {},
    userId: String = "guest",
    quizType: String = "typing"
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Core State ---
    var currentClipIndex by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var userInput by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }
    var isSessionComplete by remember { mutableStateOf(false) }
    
    // --- Stats State ---
    var heartsLeft by remember { mutableIntStateOf(10) }
    var comboCount by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalAttempted by remember { mutableIntStateOf(0) }

    // --- UI/Animation State ---
    var confettiState by remember { mutableStateOf<List<Party>>(emptyList()) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    var showStyleDialog by remember { mutableStateOf(false) }

    // --- Player Setup ---
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    LingoLog.e("VideoPlayer", "ExoPlayer Error: ${error.message}")
                }
            })
        }
    }

    // Handle Player Lifecycle & Logic
    LaunchedEffect(playbackSpeed) { exoPlayer.setPlaybackSpeed(playbackSpeed) }
    
    LaunchedEffect(videoUri, clips, currentClipIndex) {
        try {
            val targetUri = clips?.getOrNull(currentClipIndex)?.videoUri ?: videoUri
            if (exoPlayer.currentMediaItem?.localConfiguration?.uri != targetUri) {
                exoPlayer.setMediaItem(MediaItem.fromUri(targetUri))
                exoPlayer.prepare()
            }
            clips?.getOrNull(currentClipIndex)?.let { clip ->
                exoPlayer.seekTo(clip.startTimeMs)
                exoPlayer.play()
            }
        } catch (e: Exception) {
            LingoLog.e("VideoPlayer", "Playback error", e)
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.release()
        }
    }

    // --- UI Layout ---
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = (clips == null) } },
            modifier = Modifier.fillMaxSize()
        )

        if (isQuizMode && !isSessionComplete) {
            QuizHud(heartsLeft = heartsLeft, comboCount = comboCount)
        }

        if (confettiState.isNotEmpty()) {
            KonfettiView(modifier = Modifier.fillMaxSize(), parties = confettiState)
        }

        // Action Buttons Overlay
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Top Bar
            Row(modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }
            
            Row(modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()) {
                IconButton(onClick = { showStyleDialog = true }, modifier = Modifier.background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))) {
                    Icon(Icons.Default.Palette, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { playbackSpeed = if (playbackSpeed == 1.0f) 0.7f else 1.0f },
                    modifier = Modifier.background(if (playbackSpeed < 1.0f) Color.Orange else Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.SlowMotionVideo, null, tint = Color.White)
                }
            }

            // Bottom Area
            if (isQuizMode && !isSessionComplete && clips != null && currentClipIndex < clips.size) {
                val clip = clips[currentClipIndex]
                val quizData = remember(clip) { GameLogic.prepareQuiz(clip.text, difficulty) }
                
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                    QuizInteractionArea(
                        words = quizData.words,
                        hiddenIndices = quizData.hiddenIndices,
                        userInput = userInput,
                        onUserInputChange = { userInput = it },
                        isChecked = isChecked,
                        onCheck = {
                            isChecked = true
                            if (GameLogic.checkAnswer(userInput, quizData.words, quizData.hiddenIndices)) {
                                correctCount++; comboCount++; onCorrectAnswer(10)
                                flashColor = Color.Green
                                SoundManager.playCorrect(context)
                            } else {
                                heartsLeft--; comboCount = 0
                                flashColor = Color.Red
                                SoundManager.playWrong(context)
                            }
                            scope.launch { delay(800); flashColor = Color.Transparent }
                        },
                        onNext = {
                            totalAttempted++
                            if (currentClipIndex < clips.size - 1) currentClipIndex++ else isSessionComplete = true
                            userInput = ""; isChecked = false
                        }
                    )
                }
            }
        }

        if (isSessionComplete) {
            CompletionOverlay(
                correctCount = correctCount,
                totalAttempted = totalAttempted,
                difficulty = difficulty,
                onRestart = { isSessionComplete = false; currentClipIndex = 0; heartsLeft = 10; correctCount = 0 },
                onBack = onBack
            )
        }
    }
}

@Composable
fun QuizInteractionArea(
    words: List<String>,
    hiddenIndices: Set<Int>,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    isChecked: Boolean,
    onCheck: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Text display with masks
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
                words.forEachIndexed { index, word ->
                    Text(
                        text = if (hiddenIndices.contains(index) && !isChecked) "____" else word,
                        color = if (hiddenIndices.contains(index)) Color.Yellow else Color.White,
                        modifier = Modifier.padding(horizontal = 2.dp),
                        fontSize = 18.sp
                    )
                }
            }

            if (!isChecked) {
                TextField(
                    value = userInput,
                    onValueChange = onUserInputChange,
                    placeholder = { Text("הקלד את המילים החסרות") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = onCheck) { Icon(Icons.Default.Check, null) } }
                )
            } else {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text("המשך")
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        }
    }
}
