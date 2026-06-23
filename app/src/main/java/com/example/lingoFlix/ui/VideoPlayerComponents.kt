package com.example.lingoFlix.ui

import android.net.Uri
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
import com.example.lingoFlix.ui.theme.*

// Colors from LingoComponents
val DuoGreen = Color(0xFF58CC02)
val DuoDarkGreen = Color(0xFF46A302)
val DuoBlue = Color(0xFF1CB0F6)

@Composable
fun ResumeDialog(
    savedIndex: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("המשך תרגול") },
        text = { Text("נראה שהיית באמצע התרגול. האם להמשיך ממשפט ${savedIndex + 1}?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("המשך")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("התחל מהתחלה")
            }
        }
    )
}

@Composable
fun SubtitleStyleDialog(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    isBold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    colorHex: String,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("עיצוב כתוביות") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("גודל טקסט: ${fontSize.toInt()}")
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 16f..60f
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("סוג גופן:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("SansSerif", "Serif", "Monospace", "Cursive").forEach { font ->
                        FilterChip(
                            selected = fontFamily == font,
                            onClick = { onFontFamilyChange(font) },
                            label = { Text(font, fontSize = 10.sp) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBold, onCheckedChange = onBoldChange)
                    Text("טקסט מודגש (Bold)")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("צבע טקסט:")
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("#FFFFFF", "#FFFF00", "#00FF00", "#FF0000", "#00FFFF", "#FF00FF", "#FFA500", "#A9A9A9").forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(40.dp)
                                .background(Color(android.graphics.Color.parseColor(color)), CircleShape)
                                .border(if (colorHex == color) 3.dp else 1.dp, if (colorHex == color) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                                .clickable { onColorChange(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("שמור")
            }
        }
    )
}

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
                onClick = onBack,
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
                onClick = onPlayAgain,
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
}

@Composable
fun GameOverDialog(
    completedCount: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("המשחק נגמר! 💔") },
        text = { Text("השתמשת בכל הלבבות שלך. השלמת $completedCount משפטים.") },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("נסה שוב")
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text("חזור לרשימה")
            }
        }
    )
}

@Composable
fun SubtitleSearchDialog(
    videoUri: Uri,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onGoogleSearch: () -> Unit,
    onKtuvitSearch: () -> Unit,
    onImportFile: () -> Unit,
    onGenerateAI: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("חיפוש כתוביות") },
        text = {
            Column {
                TextField(
                    value = searchQuery, 
                    onValueChange = onSearchQueryChange, 
                    placeholder = { Text("שם הסרט/סדרה") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onGoogleSearch,
                    modifier = Modifier.fillMaxWidth()
                ) { 
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("חפש ב-Google") 
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onKtuvitSearch,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { 
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(8.dp))
                    Text("חפש ב-Ktuvit (מומלץ)") 
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onImportFile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { 
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ייבא קובץ SRT קיים") 
                }
                
                if (videoUri.scheme == "file") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onGenerateAI,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("ייצר בעזרת AI (לוקח זמן)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("סגור") } }
    )
}

@Composable
fun SyncTestDialog(
    onSeek: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בדיקת סנכרון כתוביות") },
        text = {
            Column {
                Text("האם הכתוביות תואמות לסרטון?")
                Text("נבדוק 3 נקודות זמן שונות.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("הבנתי")
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSeek(0.1f) }) { Text("בדוק התחלה (10%)") }
                Button(onClick = { onSeek(0.5f) }) { Text("בדוק אמצע (50%)") }
                Button(onClick = { onSeek(0.9f) }) { Text("בדוק סוף (90%)") }
            }
        }
    )
}
