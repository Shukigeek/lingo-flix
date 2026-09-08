package com.example.lingoFlix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lingoFlix.utils.OfflineTranslator
import com.example.lingoFlix.utils.LingoLog
import android.content.Context

@Composable
fun DifficultySelectionDialog(onDismiss: () -> Unit, onStart: (String, String) -> Unit) {
    var selectedDifficulty by remember { mutableStateOf("קל") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הגדרות תרגול") },
        text = {
            Column {
                Text("בחר רמת קושי:", fontWeight = FontWeight.Bold)
                listOf("קל", "בינוני", "קשה").forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDifficulty = level }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                        Text(text = level, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = when(selectedDifficulty) {
                        "קל" -> "מילה אחת חסרה בכל משפט"
                        "בינוני" -> "כ-40% מהמילים יוסתרו"
                        else -> "רוב המילים יוסתרו"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selectedDifficulty, "typing") }) {
                Text("התחל תרגול")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול")
            }
        }
    )
}

@Composable
fun XRayDialog(
    line: String, 
    missingWords: String = "", 
    sourceLang: String = "אנגלית",
    onDismiss: () -> Unit, 
    userId: String = "guest"
) {
    val context = LocalContext.current
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFromCache by remember { mutableStateOf(false) }

    LaunchedEffect(line, missingWords) {
        val cacheKey = "off_${line}_$missingWords"
        val cachePrefs = context.getSharedPreferences("xray_cache_$userId", Context.MODE_PRIVATE)
        val cached = cachePrefs.getString(cacheKey, null)
        
        if (cached != null) {
            analysisResult = cached
            isFromCache = true
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val langCode = OfflineTranslator.mapLanguage(sourceLang)
            val resultText = if (missingWords.isNotEmpty()) {
                OfflineTranslator.getWordInfo(missingWords, langCode)
            } else {
                val translation = OfflineTranslator.translate(line, langCode)
                "**תרגום:**\n$translation"
            }
            
            analysisResult = resultText
            if (resultText != null) {
                cachePrefs.edit().putString(cacheKey, resultText).apply()
            }
            isLoading = false
        } catch (e: Exception) {
            LingoLog.e("CommonDialogs", "X-Ray Analysis failed", e)
            error = "שגיאה: ${e.localizedMessage}\nוודא שיש חיבור להורדת חבילת השפה בשימוש ראשון."
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Offline Analysis", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth()) {
                if (isLoading) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("מנתח את המילה (אופליין)...", modifier = Modifier.padding(top = 8.dp))
                    }
                } else if (error != null) {
                    Text(error!!, color = Color.Red)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            analysisResult?.let { result ->
                                result.split("\n").forEach { line ->
                                    if (line.startsWith("###")) {
                                        Text(
                                            text = line.replace("###", "").trim(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    } else if (line.startsWith("•")) {
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                        )
                                    } else if (line.startsWith("**") || line.contains("**")) {
                                        // Simple bold support
                                        Text(
                                            text = line.replace("**", ""),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (line.startsWith("*") && line.endsWith("*")) {
                                        Text(
                                            text = line.replace("*", ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    } else {
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("הבנתי") }
        }
    )
}

@Composable
fun WordDetailsDialog(
    word: String,
    sourceLang: String = "אנגלית",
    onDismiss: () -> Unit,
    onAddToLearning: (String, String) -> Unit
) {
    val context = LocalContext.current
    var translation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(word) {
        val langCode = OfflineTranslator.mapLanguage(sourceLang)
        translation = OfflineTranslator.translate(word, langCode)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(word, style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("תרגום: ${translation ?: "לא נמצא"}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("תרצה להוסיף את המילה למחסן המילים שלך?", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    translation?.let { onAddToLearning(word, it) }
                    onDismiss()
                },
                enabled = !isLoading && translation != null
            ) {
                Text("הוסף ללמידה")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול")
            }
        }
    )
}
