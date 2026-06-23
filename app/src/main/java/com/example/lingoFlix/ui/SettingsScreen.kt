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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentGeminiApiKey: String,
    currentTmdbApiKey: String,
    currentAnthropicApiKey: String = "",
    onSaveKeys: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var geminiApiKey by remember { mutableStateOf(currentGeminiApiKey) }
    var tmdbApiKey by remember { mutableStateOf(currentTmdbApiKey) }
    var anthropicApiKey by remember { mutableStateOf(currentAnthropicApiKey) }

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
                label = { Text("Gemini API Key (עבור כתוביות AI)") },
                placeholder = { Text("הזן מפתח כאן...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tmdbApiKey,
                onValueChange = { tmdbApiKey = it },
                label = { Text("TMDB API Key (עבור גילוי תוכן)") },
                placeholder = { Text("הזן מפתח TMDB כאן...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = anthropicApiKey,
                onValueChange = { anthropicApiKey = it },
                label = { Text("Anthropic API Key (בונה הסקילים)") },
                placeholder = { Text("הזן מפתח Anthropic כאן...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    onSaveKeys(geminiApiKey, tmdbApiKey, anthropicApiKey)
                    Toast.makeText(context, "המפתחות נשמרו!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("שמור מפתחות")
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
                        "1. Gemini API: משמש לייצור כתוביות (SRT) לסרטונים המקומיים שלך.\n" +
                        "2. TMDB API: משמש לחיפוש סדרות וסרטים בטאב 'גילוי'.\n\n" +
                        "ניתן להשיג את המפתחות בחינם באתרים של Google AI Studio ו-TheMovieDB.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "גרסה: 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
