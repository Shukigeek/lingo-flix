package com.example.lingoFlix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.utils.LingoLog
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party

@Composable
fun VideoPlayerScreenUI(
    exoPlayer: ExoPlayer,
    confettiState: List<Party>,
    isGeneratingSubtitles: Boolean,
    generationProgress: String,
    isQuizMode: Boolean,
    isSessionComplete: Boolean,
    heartsLeft: Int,
    comboCount: Int,
    isFullScreen: Boolean,
    onFullScreenToggle: () -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedToggle: () -> Unit,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onStyleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBack: () -> Unit,
    clips: List<SubtitleClip>?,
    subtitlesVisible: Boolean,
    currentClipIndex: Int,
    favoriteClips: Set<String>,
    onToggleFavorite: (String) -> Unit,
    hiddenIndices: Set<Int>,
    wordsList: List<String>,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    isChecked: Boolean,
    onCheck: () -> Unit,
    detectedLanguage: String,
    subtitleColorHex: String,
    subtitleFontSize: Float,
    subtitleIsBold: Boolean,
    currentFontFamily: FontFamily,
    shakeOffset: Float,
    flashColor: Color,
    correctCount: Int,
    totalAttempted: Int,
    difficulty: String,
    onPlayAgain: () -> Unit,
    onReplayClip: () -> Unit,
    onNextClip: () -> Unit,
    onShowXRay: (String, String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Video Player (Base Layer)
        AndroidView(
            factory = { context ->
                LingoLog.d("VideoPlayerScreenUI", "Creating PlayerView, isQuizMode=$isQuizMode")
                PlayerView(context).apply {
                    player = exoPlayer
                    // Disable default controllers in Quiz mode to prevent interference
                    // In regular mode, we might want them, but they can block Compose UI
                    useController = !isQuizMode 
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                // Update controller visibility if mode changes
                view.useController = !isQuizMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Confetti (Visual Effect - Behind UI to not block touches)
        if (confettiState.isNotEmpty()) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = confettiState
            )
        }

        // 3. UI Layer (Interactive Layer - Always on top of Video/Confetti)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f) // Ensure it's above native view layers
        ) {
            // HUD / Top Controls
            if (isQuizMode) {
                QuizHUD(heartsLeft = heartsLeft, comboCount = comboCount)
            }
            
            TopControls(
                isFullScreen = isFullScreen,
                onFullScreenToggle = onFullScreenToggle,
                playbackSpeed = playbackSpeed,
                onPlaybackSpeedToggle = onPlaybackSpeedToggle,
                isPlaying = isPlaying,
                onPlayPauseToggle = onPlayPauseToggle,
                onStyleClick = onStyleClick,
                onSearchClick = onSearchClick,
                onBack = onBack
            )
            
            // Language Badge
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "שפה: $detectedLanguage",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // X-Ray button (if available)
            if (!isQuizMode && clips != null && currentClipIndex < clips.size) {
                 Row(
                     modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                     horizontalArrangement = Arrangement.End
                 ) {
                     IconButton(
                         onClick = { 
                             LingoLog.d("VideoPlayerScreenUI", "X-Ray clicked")
                             onShowXRay(clips[currentClipIndex].text, "") 
                         },
                         modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                     ) {
                         Icon(Icons.Outlined.Psychology, contentDescription = "X-Ray", tint = Color.White)
                     }
                 }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Subtitles & Quiz
            if (subtitlesVisible && clips != null && currentClipIndex < clips.size) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Favorite button for the current clip
                    val clip = clips[currentClipIndex]
                    val clipId = "${clip.startTimeMs}_${clip.text.hashCode()}"
                    val isFavorite = favoriteClips.contains(clipId)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { 
                                LingoLog.d("VideoPlayerScreenUI", "Toggle favorite for: $clipId")
                                onToggleFavorite(clipId) 
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else Color.White
                            )
                        }
                    }

                    SubtitleSection(
                        text = clips[currentClipIndex].text,
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
                        shakeOffset = shakeOffset,
                        flashColor = flashColor
                    )
                    
                    if (isQuizMode) {
                        val allCorrect = isChecked && hiddenIndices.all { idx -> 
                            userInput.split(Regex("\\s+")).any { it.trim().equals(wordsList[idx].trim(), ignoreCase = true) } 
                        }
                        val hiddenWords = hiddenIndices.joinToString(" ") { wordsList[it] }
                        
                        QuizInputSection(
                            userInput = userInput,
                            onUserInputChange = onUserInputChange,
                            onCheck = onCheck,
                            onSkip = onNextClip,
                            onReplay = onReplayClip,
                            isChecked = isChecked,
                            allCorrect = allCorrect,
                            hiddenWords = hiddenWords
                        )
                    }
                }
            }

            // Action buttons for Quiz (visible after checking)
            if (isChecked && !isSessionComplete && isQuizMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            LingoLog.d("VideoPlayerScreenUI", "Next clip clicked")
                            onNextClip()
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58CC02)) // DuoGreen
                    ) {
                        Text("המשך")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. Overlays (Modal Layers - Top Level)
        if (isGeneratingSubtitles) {
            Box(modifier = Modifier.zIndex(10f)) {
                GenerationOverlay(progress = generationProgress)
            }
        }
        
        if (isSessionComplete) {
            Box(modifier = Modifier.zIndex(10f)) {
                SessionCompleteOverlay(
                    correctCount = correctCount,
                    totalAttempted = totalAttempted,
                    difficulty = difficulty,
                    onPlayAgain = onPlayAgain,
                    onBack = onBack
                )
            }
        }
    }
}
