package com.example.lingoFlix.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.utils.LingoLog

@Composable
fun MetadataEditDialog(
    metadata: VideoMetadata,
    onDismiss: () -> Unit,
    onSave: (VideoMetadata) -> Unit,
    onDeleteFile: () -> Unit,
    onRenameFile: () -> Unit
) {
    var title by remember { mutableStateOf(metadata.title ?: "") }
    var season by remember { mutableStateOf(metadata.season?.toString() ?: "") }
    var episode by remember { mutableStateOf(metadata.episode?.toString() ?: "") }
    var description by remember { mutableStateOf(metadata.description ?: "") }
    
    LaunchedEffect(Unit) {
        try {
            LingoLog.d("MetadataEditDialog", "Opening dialog for: ${metadata.title}")
        } catch (e: Exception) {
            LingoLog.e("MetadataEditDialog", "Error in MetadataEditDialog", e)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("פרטי סרטון") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("כותרת") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(value = season, onValueChange = { season = it }, label = { Text("עונה") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(value = episode, onValueChange = { episode = it }, label = { Text("פרק") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text("תיאור") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        LingoLog.i("MetadataEditDialog", "Rename file requested")
                        onRenameFile()
                    }) { Icon(Icons.Default.Edit, null); Text("שנה שם קובץ") }
                    TextButton(onClick = {
                        LingoLog.i("MetadataEditDialog", "Delete file requested")
                        onDeleteFile()
                    }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Icon(Icons.Default.Delete, null); Text("מחק קובץ") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                LingoLog.i("MetadataEditDialog", "Saving metadata")
                onSave(metadata.copy(
                    title = title.ifBlank { null },
                    season = season.toIntOrNull(),
                    episode = episode.toIntOrNull(),
                    description = description.ifBlank { null }
                ))
            }) { Text("שמור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

@Composable
fun DifficultySelectionDialog(
    onDifficultySelected: (String) -> Unit,
    onRegularView: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDifficulty by remember { mutableStateOf("קל") }
    
    val difficultyLevels = listOf(
        DifficultyLevel("קל", "מתחילים", "מילה אחת חסרה בכל משפט", Color(0xFF58CC02), Icons.Default.SentimentSatisfied),
        DifficultyLevel("בינוני", "בינוני", "כ-40% מהמילים יוסתרו", Color(0xFFFFC107), Icons.Default.SentimentNeutral),
        DifficultyLevel("קשה", "מתקדם", "רוב המילים יוסתרו", Color(0xFFE91E63), Icons.Default.SentimentVeryDissatisfied)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "רמת קושי לתרגול", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                difficultyLevels.forEach { level ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDifficulty = level.id },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDifficulty == level.id) 
                                level.color.copy(alpha = 0.15f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = if (selectedDifficulty == level.id) 
                            androidx.compose.foundation.BorderStroke(2.dp, level.color) 
                        else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(level.color, CircleShape)
                                    .size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(level.icon, null, tint = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(level.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(level.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            
                            RadioButton(
                                selected = selectedDifficulty == level.id,
                                onClick = { selectedDifficulty = level.id },
                                colors = RadioButtonDefaults.colors(selectedColor = level.color)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    LingoLog.i("DifficultySelectionDialog", "Difficulty selected: $selectedDifficulty")
                    onDifficultySelected(selectedDifficulty)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = difficultyLevels.find { it.id == selectedDifficulty }?.color ?: MaterialTheme.colorScheme.primary
                )
            ) { 
                Text("התחל תרגול", fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(
                onClick = onRegularView,
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text("צפייה רגילה (ללא תרגול)", color = Color.Gray) 
            }
        }
    )
}

data class DifficultyLevel(
    val id: String,
    val title: String,
    val description: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun NewFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("תיקייה חדשה") },
        text = { TextField(value = folderName, onValueChange = { folderName = it }, label = { Text("שם התיקייה") }) },
        confirmButton = { 
            Button(onClick = {
                LingoLog.i("NewFolderDialog", "Creating folder: $folderName")
                onConfirm(folderName)
            }) { Text("צור") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}
