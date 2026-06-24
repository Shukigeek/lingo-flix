package com.example.lingoFlix.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.ui.components.DuoButton
import com.example.lingoFlix.utils.LingoLog

// Colors from LingoComponents
val DuoGreen = Color(0xFF58CC02)
val DuoDarkGreen = Color(0xFF46A302)
val DuoBlue = Color(0xFF1CB0F6)

@Composable
fun GenerationOverlay(progress: String) {
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
                text = progress,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun QuizHUD(
    heartsLeft: Int,
    comboCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(10) { index ->
                val isLost = index >= heartsLeft
                val scale by animateFloatAsState(if (isLost) 0.8f else 1.2f, label = "heartScale")
                val alpha by animateFloatAsState(if (isLost) 0.3f else 1f, label = "heartAlpha")
                
                Icon(
                    imageVector = if (isLost) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                    contentDescription = "Heart",
                    tint = if (isLost) Color.Gray else Color.Red,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(scale)
                        .alpha(alpha)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }

        if (comboCount >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val multiplier = when {
                    comboCount >= 10 -> 10
                    comboCount >= 5 -> 5
                    comboCount >= 3 -> 3
                    else -> 2
                }
                
                val infiniteTransition = rememberInfiniteTransition(label = "comboPulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "comboScale"
                )

                Surface(
                    color = Color(0xFFFF9600),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.scale(scale)
                ) {
                    Text(
                        text = "🔥 ${multiplier}x COMBO!",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InfoMessageOverlay(message: String) {
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
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TopControls(
    isFullScreen: Boolean,
    onFullScreenToggle: () -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedToggle: () -> Unit,
    onStyleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onStyleClick,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(Icons.Default.Palette, contentDescription = "Subtitle Style", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onPlaybackSpeedToggle,
                modifier = Modifier.background(
                    if (playbackSpeed < 1.0f) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                )
            ) {
                Icon(
                    imageVector = if (playbackSpeed < 1.0f) Icons.Default.SlowMotionVideo else Icons.Default.PlayCircle, 
                    contentDescription = "Playback Speed",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(Icons.Default.Subtitles, contentDescription = "Subtitle Search", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    LingoLog.d("TopControls", "Back clicked")
                    onBack()
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        IconButton(
            onClick = onFullScreenToggle,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
        ) {
            Icon(
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = "Toggle Fullscreen",
                tint = Color.White
            )
        }
    }
}

@Composable
fun SessionCompleteOverlay(
    correctCount: Int,
    totalAttempted: Int,
    difficulty: String,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
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
            
            val sessionXP = try {
                correctCount * when(difficulty) {
                    "בינוני" -> 30
                    "קשה" -> 40
                    else -> 20
                }
            } catch (e: Exception) {
                LingoLog.e("SessionCompleteOverlay", "Error calculating XP", e)
                0
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
                    LingoLog.i("SessionCompleteOverlay", "Play again clicked")
                    onPlayAgain()
                },
                color = DuoGreen,
                darkColor = DuoDarkGreen,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DuoButton(
                text = "חזור לבית",
                onClick = {
                    LingoLog.i("SessionCompleteOverlay", "Back home clicked")
                    onBack()
                },
                color = DuoBlue,
                darkColor = DuoBlue.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
