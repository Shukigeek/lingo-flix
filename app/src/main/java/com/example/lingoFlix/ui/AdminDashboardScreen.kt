package com.example.lingoFlix.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.model.AppConfig
import com.example.lingoFlix.model.RecommendedMedia
import com.example.lingoFlix.data.ConfigDao
import com.example.lingoFlix.data.RecommendedMediaDao
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    configDao: ConfigDao,
    recommendedMediaDao: RecommendedMediaDao
) {
    val configState = configDao.getConfig().collectAsState(initial = AppConfig())
    val currentConfig = configState.value ?: AppConfig()
    
    val mediaState = recommendedMediaDao.getAllRecommendations().collectAsState(initial = emptyList())
    val allMedia = mediaState.value
    
    val scope = rememberCoroutineScope()
    var editingMedia by remember { mutableStateOf<RecommendedMedia?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ניהול מערכת (Developer)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("הגדרות בסיס נתונים ושפה", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            // Language Management
            OutlinedTextField(
                value = currentConfig.targetLanguage,
                onValueChange = { 
                    scope.launch { 
                        try {
                            configDao.saveConfig(currentConfig.copy(targetLanguage = it)) 
                        } catch (e: Exception) {
                            LingoLog.e("AdminDashboardScreen", "Failed to save language config", e)
                        }
                    } 
                },
                label = { Text("שפת יעד (Target Language)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Discovery Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("גילוי תוכן אוטומטי (Auto-Discovery)")
                Switch(
                    checked = currentConfig.isAutoDiscoveryEnabled,
                    onCheckedChange = { 
                        scope.launch { 
                            try {
                                configDao.saveConfig(currentConfig.copy(isAutoDiscoveryEnabled = it)) 
                            } catch (e: Exception) {
                                LingoLog.e("AdminDashboardScreen", "Failed to save discovery config", e)
                            }
                        } 
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text("ניהול תוכן (Content Management)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            AdminActionButton(
                text = "הוסף המלצה ידנית (TMDB ID)",
                icon = Icons.Default.AddCircle,
                color = Color(0xFF58CC02),
                onClick = { editingMedia = RecommendedMedia(0, "", "movie") }
            )

            AdminActionButton(
                text = "סנכרון סדרות מטלגרם",
                icon = Icons.Default.CloudDownload,
                color = Color(0xFF24A1DE),
                onClick = { /* Logic for telegram sync */ }
            )

            AdminActionButton(
                text = "עדכן נתוני TMDB גלובליים",
                icon = Icons.Default.Sync,
                color = Color(0xFF58CC02),
                onClick = { /* Global TMDB Sync */ }
            )

            AdminActionButton(
                text = "נקה זיכרון מטמון (Cache)",
                icon = Icons.Default.DeleteSweep,
                color = Color.Red,
                onClick = { /* Cache Cleanup */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("ערוך המלצות קיימות", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            allMedia.forEach { media ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(media.title, fontWeight = FontWeight.Bold)
                            Text("ID: ${media.tmdbId} | ${if (media.mediaType == "tv") "סדרה" else "סרט"}", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { editingMedia = media }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text("סטטוס מערכת", fontWeight = FontWeight.Bold)
            Text("גרסת דאטא-בייס: 4", fontSize = 12.sp, color = Color.Gray)
            Text("סנכרון אחרון: ${java.util.Date(currentConfig.lastSyncTimestamp)}", fontSize = 12.sp, color = Color.Gray)
        }
    }

    if (editingMedia != null) {
        MediaEditDialog(
            media = editingMedia!!,
            onDismiss = { editingMedia = null },
            onSave = { updated ->
                scope.launch {
                    try {
                        recommendedMediaDao.insertRecommendation(updated)
                        editingMedia = null
                    } catch (e: Exception) {
                        LingoLog.e("AdminDashboardScreen", "Failed to insert recommendation", e)
                    }
                }
            },
            onDelete = {
                scope.launch {
                    try {
                        recommendedMediaDao.deleteRecommendation(editingMedia!!)
                        editingMedia = null
                    } catch (e: Exception) {
                        LingoLog.e("AdminDashboardScreen", "Failed to delete recommendation", e)
                    }
                }
            }
        )
    }
}

@Composable
fun MediaEditDialog(
    media: RecommendedMedia,
    onDismiss: () -> Unit,
    onSave: (RecommendedMedia) -> Unit,
    onDelete: () -> Unit
) {
    val isNew = media.tmdbId == 0
    var tmdbId by remember { mutableStateOf(if (isNew) "" else media.tmdbId.toString()) }
    var mediaType by remember { mutableStateOf(media.mediaType) }
    var title by remember { mutableStateOf(media.title) }
    var rating by remember { mutableStateOf(media.cachedRating.toString()) }
    var telegramLink by remember { mutableStateOf(media.customTelegramLink ?: "") }
    var youtubeId by remember { mutableStateOf(media.customYoutubeTrailerId ?: "") }
    var description by remember { mutableStateOf(media.cachedDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "הוסף המלצה חדשה" else "ערוך תוכן: ${media.title}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (isNew) {
                    OutlinedTextField(value = tmdbId, onValueChange = { tmdbId = it }, label = { Text("TMDB ID") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mediaType == "movie", onClick = { mediaType = "movie" })
                        Text("סרט")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = mediaType == "tv", onClick = { mediaType = "tv" })
                        Text("סדרה")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("כותרת") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = rating, onValueChange = { rating = it }, label = { Text("דירוג (למשל 8.5)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = telegramLink, onValueChange = { telegramLink = it }, label = { Text("קישור טלגרם") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = youtubeId, onValueChange = { youtubeId = it }, label = { Text("מזהה טריילר (YouTube ID)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("תיאור") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                if (!isNew) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("מחק המלצה זו")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalId = tmdbId.toIntOrNull() ?: media.tmdbId
                if (finalId != 0) {
                    onSave(media.copy(
                        tmdbId = finalId,
                        mediaType = mediaType,
                        title = title,
                        cachedRating = rating.toDoubleOrNull() ?: media.cachedRating,
                        customTelegramLink = telegramLink.ifBlank { null },
                        customYoutubeTrailerId = youtubeId.ifBlank { null },
                        cachedDescription = description.ifBlank { null }
                    ))
                }
            }) { Text("שמור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

@Composable
fun AdminActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.1f), contentColor = color),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}
