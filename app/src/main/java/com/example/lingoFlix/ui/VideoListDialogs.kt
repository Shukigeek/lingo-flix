package com.example.lingoFlix.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחר רמת קושי לתרגול") },
        text = {
            Column {
                listOf("קל", "בינוני", "קשה").forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedDifficulty = level }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selectedDifficulty == level, onClick = { selectedDifficulty = level })
                        Text(text = level, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                LingoLog.i("DifficultySelectionDialog", "Difficulty selected: $selectedDifficulty")
                onDifficultySelected(selectedDifficulty)
            }) { Text("התחל תרגול") }
        },
        dismissButton = {
            TextButton(onClick = {
                LingoLog.i("DifficultySelectionDialog", "Regular view selected")
                onRegularView()
            }) { Text("צפייה רגילה") }
        }
    )
}

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
