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
import com.example.lingoFlix.utils.SecurityUtils
import com.google.ai.client.generativeai.GenerativeModel
import android.content.Context

@Composable
fun DifficultySelectionDialog(onDismiss: () -> Unit, onStart: (String, String) -> Unit) {
    var selectedDifficulty by remember { mutableStateOf("קל") }
    var selectedQuizType by remember { mutableStateOf("typing") }
    
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
                
                Text("אופן תרגול:", fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedQuizType = "typing" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = selectedQuizType == "typing", onClick = { selectedQuizType = "typing" })
                    Text(text = "הקלדה", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedQuizType = "multiple_choice" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = selectedQuizType == "multiple_choice", onClick = { selectedQuizType = "multiple_choice" })
                    Text(text = "בחירה מרובה", modifier = Modifier.padding(start = 8.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
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
            Button(onClick = { onStart(selectedDifficulty, selectedQuizType) }) {
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
fun XRayDialog(line: String, onDismiss: () -> Unit, userId: String = "guest") {
    val context = LocalContext.current
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFromCache by remember { mutableStateOf(false) }

    val apiKey = remember(userId) { SecurityUtils.getUserApiKey(context, userId) ?: "" }

    LaunchedEffect(line) {
        val cachePrefs = context.getSharedPreferences("xray_cache_$userId", Context.MODE_PRIVATE)
        val cached = cachePrefs.getString(line, null)
        
        if (cached != null) {
            analysisResult = cached
            isFromCache = true
            isLoading = false
            return@LaunchedEffect
        }

        if (apiKey.isBlank()) {
            error = "נא להזין API KEY בהגדרות כדי להשתמש ב-AI X-Ray"
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )
            val prompt = """
                Analyze this sentence from a movie: "$line"
                Provide the following in Hebrew (formatted with Markdown):
                1. Natural Hebrew translation.
                2. Breakdown of key words: Translation, Part of Speech (noun, verb, etc.), and Synonyms.
                3. For verbs, provide basic conjugations (Past, Present, Future).
                4. Examples of where else these words are used.
                Keep it concise and clear.
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            val resultText = response.text
            analysisResult = resultText
            
            if (resultText != null) {
                cachePrefs.edit().putString(line, resultText).apply()
            }
            
            isLoading = false
        } catch (e: Exception) {
            error = "שגיאה בחיבור ל-AI: ${e.localizedMessage}\nוודא שיש אינטרנט והמפתח תקין."
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subtitle AI X-Ray", style = MaterialTheme.typography.headlineSmall)
                }
                if (isFromCache) {
                    Surface(color = Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("Cached", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 450.dp).fillMaxWidth()) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("מנתח את המשפט בעזרת AI...")
                    }
                } else if (error != null) {
                    Text(error!!, color = Color.Red)
                } else {
                    LazyColumn {
                        item {
                            Text(
                                text = analysisResult ?: "לא התקבל ניתוח",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("הבנתי!")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun XRaySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp).alpha(0.1f))
    }
}
