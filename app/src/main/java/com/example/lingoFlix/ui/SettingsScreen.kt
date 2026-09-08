package com.example.lingoFlix.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.lingoFlix.utils.LingoLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentGeminiApiKey: String,
    currentTmdbApiKey: String,
    currentAnthropicApiKey: String = "",
    currentOpenAiApiKey: String = "",
    onSaveKeys: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var geminiApiKey by remember { mutableStateOf(currentGeminiApiKey) }
    var tmdbApiKey by remember { mutableStateOf(currentTmdbApiKey) }
    var anthropicApiKey by remember { mutableStateOf(currentAnthropicApiKey) }
    var openAiApiKey by remember { mutableStateOf(currentOpenAiApiKey) }

    var selectedTheme by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הגדרות") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
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
            Text(
                text = "הגדרות API",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = geminiApiKey,
                onValueChange = { geminiApiKey = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tmdbApiKey,
                onValueChange = { tmdbApiKey = it },
                label = { Text("TMDB API Key") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = anthropicApiKey,
                onValueChange = { anthropicApiKey = it },
                label = { Text("Anthropic API Key") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = openAiApiKey,
                onValueChange = { openAiApiKey = it },
                label = { Text("OpenAI API Key (for Pro Subtitles)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "ערכת נושא (רקע)",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ברירת מחדל", "כחול שמיים", "ירוק טבע").forEachIndexed { index, name ->
                    FilterChip(
                        selected = selectedTheme == index,
                        onClick = { selectedTheme = index },
                        label = { Text(name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onSaveKeys(geminiApiKey, tmdbApiKey, anthropicApiKey, openAiApiKey)
                    Toast.makeText(context, "ההגדרות נשמרו!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("שמור הגדרות")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("מידע על המפתחות", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Gemini API: משמש לייצור כתוביות בסיסי.\n" +
                        "2. OpenAI API: משמש לייצור כתוביות Pro (Whisper) ברמה הגבוהה ביותר.\n" +
                        "3. TMDB API: משמש לחיפוש מידע על סרטים.\n\n" +
                        "ניתן להשיג את המפתחות באתרים של OpenAI ו-Google AI Studio.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "גרסה: 1.1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
