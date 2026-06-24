package com.example.lingoFlix.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.utils.LingoLog
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.io.File

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.SrtParser
import kotlinx.coroutines.delay
import android.util.Log

@Composable
fun VideoPlayerController(
    exoPlayer: ExoPlayer,
    playbackSpeed: Float,
    videoUri: Uri,
    clips: List<SubtitleClip>?,
    currentClipIndex: Int,
    detectedLanguage: String,
    preferredAudioLang: String,
    lifecycleOwner: LifecycleOwner,
    activity: ComponentActivity?,
    isFullScreen: Boolean,
    context: Context
) {
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
            LingoLog.d("VideoPlayerController", "Loading new media: $targetUri")
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
                val seekBack = 200L
                (clips[currentClipIndex].startTimeMs - seekBack).coerceAtLeast(0L)
            } else 0L

            exoPlayer.setMediaItem(mediaItemBuilder.build(), startPos)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
            
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage(preferredAudioLang)
                .build()

            var waited = 0
            while (exoPlayer.playbackState != androidx.media3.common.Player.STATE_READY && waited < 5000) {
                delay(50)
                waited += 50
            }
        } else if (clips != null && currentClipIndex < clips.size) {
            val currentPos = exoPlayer.currentPosition
            val clipStart = clips[currentClipIndex].startTimeMs
            val seekBack = 200L
            val targetPos = (clipStart - seekBack).coerceAtLeast(0L)
            
            if (Math.abs(currentPos - targetPos) > 500) { 
                LingoLog.d("VideoPlayerController", "Seeking to clip start: $targetPos")
                exoPlayer.seekTo(targetPos)
            }
        }

        exoPlayer.play()
    }

    // Check if clip ended
    if (clips != null) {
        LaunchedEffect(currentClipIndex, clips) {
            while (true) {
                delay(30)
                if (currentClipIndex < clips.size) {
                    val clip = clips[currentClipIndex]
                    val endBuffer = 300L
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
}

@Composable
fun SubtitleSection(
    text: String,
    isQuizMode: Boolean,
    hiddenIndices: Set<Int>,
    wordsList: List<String>,
    userInput: String,
    isChecked: Boolean,
    detectedLanguage: String,
    subtitleColorHex: String,
    subtitleFontSize: Float,
    subtitleIsBold: Boolean,
    currentFontFamily: FontFamily,
    shakeOffset: Float,
    flashColor: Color
) {
    val isRtl = detectedLanguage == "עברית"
    val textColor = try { 
        Color(android.graphics.Color.parseColor(subtitleColorHex)) 
    } catch(e: Exception) { 
        LingoLog.e("SubtitleSection", "Error parsing color: $subtitleColorHex", e)
        Color.White 
    }
    
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
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(bottom = 32.dp)
            .offset(x = shakeOffset.dp)
            .border(
                2.dp, 
                if (flashColor != Color.Transparent) flashColor else borderColor.copy(alpha = 0.3f), 
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.background(flashColor.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
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
                                        fontSize = subtitleFontSize.sp,
                                        fontWeight = if (subtitleIsBold) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = currentFontFamily,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                            textAlign = TextAlign.Center,
                                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = word,
                                        color = textColor,
                                        fontSize = subtitleFontSize.sp,
                                        fontWeight = if (subtitleIsBold) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = currentFontFamily,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                            textAlign = TextAlign.Center,
                                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = subtitleFontSize.sp,
                            fontWeight = if (subtitleIsBold) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = currentFontFamily,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizInputSection(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onCheck: () -> Unit,
    isChecked: Boolean,
    allCorrect: Boolean,
    hiddenWords: String
) {
    if (!isChecked) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = onUserInputChange,
                placeholder = { Text("הקלד את המילים החסרות...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        try {
                            LingoLog.d("QuizInputSection", "Checking input")
                            onCheck()
                        } catch (e: Exception) {
                            LingoLog.e("QuizInputSection", "Error in onCheck", e)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Check")
                    }
                }
            )
        }
    } else {
        Text(
            text = if (allCorrect) "כל הכבוד! ✨" else "המילים היו: $hiddenWords",
            color = if (allCorrect) Color.Green else Color.Red,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
