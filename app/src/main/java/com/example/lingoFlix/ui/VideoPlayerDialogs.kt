package com.example.lingoFlix.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.example.lingoFlix.ui.components.XRayDialog
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SubtitleGenerator
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VideoPlayerDialogsOverlay(
    showStyleDialog: Boolean,
    onStyleDismiss: () -> Unit,
    subtitleFontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    subtitleFontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    subtitleIsBold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    subtitleColorHex: String,
    onColorChange: (String) -> Unit,
    onStyleSave: () -> Unit,
    showXRay: Boolean,
    onXRayDismiss: () -> Unit,
    xRayLine: String,
    xRayMissingWords: String,
    detectedLanguage: String,
    userId: String,
    showSearchDialog: Boolean,
    onSearchDismiss: () -> Unit,
    videoUri: Uri,
    subtitleSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSubtitlesGenerated: () -> Unit,
    onGeneratingStateChange: (Boolean) -> Unit,
    onGenerationProgressChange: (String) -> Unit,
    showSyncTest: Boolean,
    onSyncTestDismiss: () -> Unit,
    exoPlayer: ExoPlayer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
                onSubtitlesGenerated()
            } catch (e: Exception) {
                LingoLog.e("VideoPlayerDialogsOverlay", "Error importing subtitles", e)
                Toast.makeText(context, "שגיאה בייבוא", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showStyleDialog) {
        SubtitleStyleDialog(
            fontSize = subtitleFontSize,
            onFontSizeChange = onFontSizeChange,
            fontFamily = subtitleFontFamily,
            onFontFamilyChange = onFontFamilyChange,
            isBold = subtitleIsBold,
            onBoldChange = onBoldChange,
            colorHex = subtitleColorHex,
            onColorChange = onColorChange,
            onSave = onStyleSave,
            onDismiss = onStyleDismiss
        )
    }

    if (showXRay) {
        XRayDialog(
            line = xRayLine,
            missingWords = xRayMissingWords,
            sourceLang = detectedLanguage,
            onDismiss = onXRayDismiss,
            userId = userId
        )
    }

    if (showSearchDialog) {
        SubtitleSearchDialog(
            videoUri = videoUri,
            searchQuery = subtitleSearchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onGoogleSearch = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${subtitleSearchQuery}+subtitles+srt"))) },
            onKtuvitSearch = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://www.ktuvit.me/Movie/Search?q=${subtitleSearchQuery}"))) },
            onImportFile = { importSubtitleLauncher.launch("*/*") },
            onGenerateAI = {
                scope.launch {
                    try {
                        onSearchDismiss()
                        onGeneratingStateChange(true)
                        val videoFile = File(videoUri.path!!)
                        val result = SubtitleGenerator.generateSubtitles(context, videoFile, userId) { onGenerationProgressChange(it) }
                        onGeneratingStateChange(false)
                        result.onSuccess {
                            Toast.makeText(context, "כתוביות נוצרו בהצלחה!", Toast.LENGTH_SHORT).show()
                            onSubtitlesGenerated()
                        }.onFailure {
                            LingoLog.e("VideoPlayerDialogsOverlay", "AI Subtitle generation failed", it)
                            Toast.makeText(context, "שגיאה ביצירה: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        LingoLog.e("VideoPlayerDialogsOverlay", "Exception during subtitle generation", e)
                        onGeneratingStateChange(false)
                    }
                }
            },
            onDismiss = onSearchDismiss
        )
    }

    if (showSyncTest) {
        SyncTestDialog(
            onSeek = { percentage ->
                try {
                    exoPlayer.seekTo((exoPlayer.duration * percentage).toLong())
                    exoPlayer.play()
                } catch (e: Exception) {
                    LingoLog.e("VideoPlayerDialogsOverlay", "Seek error during sync test", e)
                }
            },
            onDismiss = onSyncTestDismiss
        )
    }
}

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
            Button(onClick = {
                LingoLog.i("ResumeDialog", "Resuming from index $savedIndex")
                onConfirm()
            }) {
                Text("המשך")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                LingoLog.i("ResumeDialog", "Starting from scratch")
                onDismiss()
            }) {
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
                        val backgroundColor = try {
                            Color(android.graphics.Color.parseColor(color))
                        } catch (e: Exception) {
                            LingoLog.e("SubtitleStyleDialog", "Error parsing color: $color", e)
                            Color.White
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(40.dp)
                                .background(backgroundColor, CircleShape)
                                .border(if (colorHex == color) 3.dp else 1.dp, if (colorHex == color) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                                .clickable { onColorChange(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                LingoLog.i("SubtitleStyleDialog", "Style saved")
                onSave()
            }) {
                Text("שמור")
            }
        }
    )
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
            Button(onClick = {
                LingoLog.i("GameOverDialog", "Retry clicked")
                onRetry()
            }) {
                Text("נסה שוב")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                LingoLog.i("GameOverDialog", "Back to list clicked")
                onBack()
            }) {
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
                    onClick = {
                        LingoLog.i("SubtitleSearchDialog", "Google search: $searchQuery")
                        onGoogleSearch()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { 
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("חפש ב-Google") 
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        LingoLog.i("SubtitleSearchDialog", "Ktuvit search: $searchQuery")
                        onKtuvitSearch()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { 
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(8.dp))
                    Text("חפש ב-Ktuvit (מומלץ)") 
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        LingoLog.i("SubtitleSearchDialog", "Import file clicked")
                        onImportFile()
                    },
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
                        onClick = {
                            LingoLog.i("SubtitleSearchDialog", "Generate AI clicked")
                            onGenerateAI()
                        },
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
                Button(onClick = { 
                    LingoLog.d("SyncTestDialog", "Seek 10%")
                    onSeek(0.1f) 
                }) { Text("בדוק התחלה (10%)") }
                Button(onClick = { 
                    LingoLog.d("SyncTestDialog", "Seek 50%")
                    onSeek(0.5f) 
                }) { Text("בדוק אמצע (50%)") }
                Button(onClick = { 
                    LingoLog.d("SyncTestDialog", "Seek 90%")
                    onSeek(0.9f) 
                }) { Text("בדוק סוף (90%)") }
            }
        }
    )
}
