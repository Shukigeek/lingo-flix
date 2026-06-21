package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lingoFlix.R
import com.example.lingoFlix.data.remote.TmdbApiService
import com.example.lingoFlix.data.repository.TmdbRepository
import com.example.lingoFlix.model.SearchResult
import com.example.lingoFlix.ui.viewmodel.DiscoveryUiState
import com.example.lingoFlix.ui.viewmodel.DiscoveryViewModel
import com.example.lingoFlix.ui.viewmodel.DiscoveryViewModelFactory
import com.example.lingoFlix.utils.SecurityUtils
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(userId: String) {
    val context = LocalContext.current
    
    val apiService = remember { TmdbApiService.create() }
    val repository = remember { TmdbRepository(apiService) }
    val viewModel: DiscoveryViewModel = viewModel(
        factory = DiscoveryViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchResult?>(null) }
    val tmdbApiKey = remember { SecurityUtils.getTmdbApiKey(context, userId) }

    // Mock "Continue Watching" data for UI placeholder
    val continueWatching = remember {
        listOf(
            SearchResult(1, "Friends", "https://image.tmdb.org/t/p/w500/fob2vYm5998Ar936IIn9C9YpXcr.jpg", "", "tv"),
            SearchResult(2, "Breaking Bad", "https://image.tmdb.org/t/p/w500/ggm8bbub63OwoE1Z977jZ0hzR3H.jpg", "", "tv")
        )
    }

    LaunchedEffect(Unit) {
        if (tmdbApiKey != null) {
            viewModel.fetchTrending(tmdbApiKey)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "גלה תוכן חדש",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                if (tmdbApiKey != null) {
                    viewModel.search(it, tmdbApiKey)
                }
            },
            placeholder = { Text("חפש סרט או סדרה...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (query.isEmpty()) {
            Text("המשך בצפייה", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(continueWatching) { item ->
                    ContinueWatchingCard(item) { selectedItem = item }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("מומלצים עבורך", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            if (tmdbApiKey.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("הזן מפתח TMDB בהגדרות כדי להתחיל", color = Color.Gray)
                }
            } else {
                when (val state = uiState) {
                    is DiscoveryUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is DiscoveryUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                    is DiscoveryUiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(state.results) { item ->
                                DiscoveryCard(item) { selectedItem = item }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (selectedItem != null) {
        val item = selectedItem!!
        
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF141414),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    if (item.youtubeVideoId != null) {
                        YoutubePlayer(
                            videoId = item.youtubeVideoId,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF141414))))
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Movie, null, modifier = Modifier.size(64.dp), tint = Color.White)
                        }
                    }
                    
                    IconButton(
                        onClick = { selectedItem = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (item.mediaType == "tv") "סדרה" else "סרט", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${String.format("%.1f", item.rating)} ⭐", color = Color(0xFF46D369), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = item.description.ifEmpty { "אין תיאור זמין עבור תוכן זה." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("צפייה בטלגרם:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    ExternalActionCard(
                        title = "חפש בטלגרם",
                        subtitle = "חיפוש ישיר עבור ${item.title}",
                        icon = Icons.AutoMirrored.Filled.Send,
                        color = Color(0xFF24A1DE)
                    ) {
                        val encodedQuery = URLEncoder.encode(item.title, "UTF-8")
                        val telegramUri = Uri.parse("tg://search?text=$encodedQuery")
                        try { 
                            context.startActivity(Intent(Intent.ACTION_VIEW, telegramUri)) 
                        } catch (e: Exception) {
                            // Fallback to web search if Telegram app is not installed
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/s/search?q=$encodedQuery")))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun YoutubePlayer(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
                loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&mute=0")
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 12.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(item: SearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Progress bar overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun DiscoveryCard(item: SearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
                Icon(
                    Icons.Default.Movie, 
                    null, 
                    modifier = Modifier.size(48.dp).align(Alignment.Center), 
                    tint = Color.White.copy(alpha = 0.2f)
                )
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )
            
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
