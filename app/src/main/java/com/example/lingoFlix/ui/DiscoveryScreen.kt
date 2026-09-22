package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.data.remote.TmdbApiService
import com.example.lingoFlix.data.repository.TmdbRepository
import com.example.lingoFlix.data.repository.RecommendedMediaRepository
import com.example.lingoFlix.model.SearchResult
import com.example.lingoFlix.ui.discovery.*
import com.example.lingoFlix.ui.viewmodel.DiscoveryUiState
import com.example.lingoFlix.ui.viewmodel.DiscoveryViewModel
import com.example.lingoFlix.ui.viewmodel.DiscoveryViewModelFactory
import com.example.lingoFlix.utils.LingoLog
import com.example.lingoFlix.utils.SecurityUtils
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    userId: String,
    isServerOnline: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val database = remember { AppDatabase.getDatabase(context) }
    val apiService = remember { TmdbApiService.create() }
    val lingoApiService = remember { com.example.lingoFlix.api.LingoApiService.create() }
    
    var cloudMovies by remember { mutableStateOf<List<com.example.lingoFlix.api.CloudMovie>>(emptyList()) }
    
    val tmdbRepository = remember { TmdbRepository(apiService) }
    val recommendedRepository = remember { RecommendedMediaRepository(database.recommendedMediaDao(), apiService) }
    
    val viewModel: DiscoveryViewModel = viewModel(
        factory = DiscoveryViewModelFactory(tmdbRepository, recommendedRepository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val developerPicks by viewModel.developerRecommendations.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchResult?>(null) }
    val tmdbApiKey = remember { SecurityUtils.getTmdbApiKey(context, userId) }

    LaunchedEffect(tmdbApiKey, isServerOnline) {
        if (!tmdbApiKey.isNullOrBlank()) {
            viewModel.fetchTrending(tmdbApiKey)
        }
        if (isServerOnline) {
            try {
                cloudMovies = lingoApiService.getCloudLibrary()
            } catch (e: Exception) {
                LingoLog.e("DiscoveryScreen", "Failed to fetch cloud library", e)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "גלה תוכן חדש", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                if (!tmdbApiKey.isNullOrBlank()) viewModel.search(it, tmdbApiKey)
            },
            placeholder = { Text("חפש סרט או סדרה...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha = 0.9f), unfocusedContainerColor = Color.White.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (query.isEmpty()) {
            // Content Sources Section
            Text("מקורות תוכן חיצוניים", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    SourceCard(
                        name = "YouTube",
                        icon = Icons.Default.PlayCircle,
                        color = Color(0xFFFF0000)
                    ) {
                        launchExternalSearch(context, "https://www.youtube.com/results?search_query=")
                    }
                }
                item {
                    SourceCard(
                        name = "Netflix",
                        icon = Icons.Default.Movie,
                        color = Color(0xFFE50914)
                    ) {
                        launchExternalSearch(context, "https://www.netflix.com/search?q=")
                    }
                }
                item {
                    SourceCard(
                        name = "Telegram",
                        icon = Icons.Default.Send,
                        color = Color(0xFF24A1DE)
                    ) {
                        launchTelegramSearch(context, "")
                    }
                }
                item {
                    SourceCard(
                        name = "Torrent",
                        icon = Icons.Default.FileDownload,
                        color = Color(0xFF4CAF50)
                    ) {
                        launchExternalSearch(context, "https://1337x.to/search/")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (cloudMovies.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF58CC02), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("הספרייה הפרטית בשרת (Cloud)", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cloudMovies) { movie ->
                        CloudMovieCard(movie) {
                            // Logic to request this movie from server
                            Toast.makeText(context, "מבקש את '${movie.title}' מהשרת...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (developerPicks.isNotEmpty()) {
                Text("המלצות המפתח (מומלץ!)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(developerPicks) { item -> RecommendationCard(item) { selectedItem = item } }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text("מגמות עכשיו", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            if (tmdbApiKey.isNullOrBlank() && query.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("הזן מפתח TMDB בהגדרות כדי לחפש", color = Color.Gray) }
            } else {
                when (val state = uiState) {
                    is DiscoveryUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is DiscoveryUiState.Error -> DiscoveryErrorState(state.message, query)
                    is DiscoveryUiState.Success -> DiscoverySuccessState(state.results, query) { selectedItem = it }
                    else -> {}
                }
            }
        }
    }

    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF141414),
            contentColor = Color.White
        ) {
            DiscoveryDetailContent(
                item = selectedItem!!, 
                onDismiss = { selectedItem = null }, 
                onAddToRecommendations = { viewModel.addToSystemRecommendations(it) },
                isServerOnline = isServerOnline
            )
        }
    }
}

@Composable
fun DiscoveryErrorState(message: String, query: String) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        if (query.isNotEmpty()) {
            Button(
                onClick = { launchTelegramSearch(context, query) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24A1DE))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("חפש \"$query\" בטלגרם")
            }
        }
    }
}

@Composable
fun DiscoverySuccessState(results: List<SearchResult>, query: String, onItemSelected: (SearchResult) -> Unit) {
    val context = LocalContext.current
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("תוצאות חיפוש (${results.size})", color = Color.Gray, fontSize = 14.sp)
            if (query.isNotEmpty()) {
                TextButton(onClick = { launchTelegramSearch(context, query) }) { Text("לא מצאת? חפש בטלגרם", fontSize = 12.sp) }
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(results) { item -> DiscoveryCard(item) { onItemSelected(item) } }
        }
    }
}

@Composable
fun SourceCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(140.dp).height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        }
    }
}

private fun launchExternalSearch(context: android.content.Context, baseUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "לא ניתן לפתוח את הקישור", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TelegramBotCard(
    name: String,
    handle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(handle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private fun launchTelegramBot(context: android.content.Context, botName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$botName"))
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$botName")))
    }
}

private fun launchTelegramSearch(context: android.content.Context, query: String) {
    try {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val telegramUri = Uri.parse("tg://search?text=$encodedQuery")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, telegramUri))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/s/search?q=$encodedQuery")))
        }
    } catch (e: Exception) {
        LingoLog.e("DiscoveryScreen", "Failed to launch Telegram search", e)
    }
}

@Composable
fun CloudMovieCard(movie: com.example.lingoFlix.api.CloudMovie, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = movie.image_url,
                contentDescription = null,
                modifier = Modifier.height(200.dp).fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(movie.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(movie.category, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "רמה: ${movie.difficulty}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
