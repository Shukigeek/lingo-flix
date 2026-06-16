package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    // Using viewModel() with a factory to inject the repository
    val apiService = remember { TmdbApiService.create() }
    val repository = remember { TmdbRepository(apiService) }
    val viewModel: DiscoveryViewModel = viewModel(
        factory = DiscoveryViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchResult?>(null) }
    val tmdbApiKey = remember { SecurityUtils.getTmdbApiKey(context, userId) }

    // Hardcoded fallback data for preview/offline
    val fallbackResults = remember {
        listOf(
            SearchResult(1, "Friends", "https://image.tmdb.org/t/p/w500/fob2vYm5998Ar936IIn9C9YpXcr.jpg", "החיים, האהבות והצחוקים של שישה חברים בניו יורק.", "tv"),
            SearchResult(2, "Breaking Bad", "https://image.tmdb.org/t/p/w500/ggm8bbub63OwoE1Z977jZ0hzR3H.jpg", "מורה לכימיה הופך ליצרן סמים כדי להציל את משפחתו.", "tv"),
            SearchResult(3, "The Office", "https://image.tmdb.org/t/p/w500/q979SsbMqUnwh7ZzQm07p5vU4lc.jpg", "חיי היומיום המצחיקים במשרד למכירת נייר.", "tv"),
            SearchResult(4, "Stranger Things", "https://image.tmdb.org/t/p/w500/49Wfivq1TdZ0VE6US7zAhS6FD9w.jpg", "תעלומות על-טבעיות בעיירה קטנה בשנות ה-80.", "tv"),
            SearchResult(5, "Game of Thrones", "https://image.tmdb.org/t/p/w500/7WsyChvRStv9OidaxPFEj739vvk.jpg", "מאבקי כוח אפיים על כס הברזל.", "tv"),
            SearchResult(6, "The Bear", "https://image.tmdb.org/t/p/w500/5NX98f73YQzR14CgYfI6nFvR6U9.jpg", "שף צעיר חוזר לנהל את העסק המשפחתי בשיקגו.", "tv")
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
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
            placeholder = { Text("חפש סדרה...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (tmdbApiKey.isNullOrBlank() && query.isEmpty()) {
            // Show Fallback results when no API key
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(fallbackResults) { item ->
                    DiscoveryCard(item) { selectedItem = item }
                }
            }
        } else if (tmdbApiKey.isNullOrBlank() && query.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("הזן מפתח TMDB בהגדרות כדי לחפש", color = Color.Gray)
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
                Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Movie, null, modifier = Modifier.size(64.dp), tint = Color.White)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF141414))))
                    )
                    
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
                        Text("8.5 ⭐", color = Color(0xFF46D369), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(item.description, style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("איפה לצפות / לחפש:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExternalActionCard(
                            title = "חפש ישירות בטלגרם",
                            subtitle = "מצא קבצים וקבוצות להורדה",
                            icon = Icons.AutoMirrored.Filled.Send,
                            color = Color(0xFF24A1DE)
                        ) {
                            val telegramUri = Uri.parse("tg://search?text=${URLEncoder.encode(item.title, "UTF-8")}")
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, telegramUri)) } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/s/search?q=${URLEncoder.encode(item.title, "UTF-8")}")))
                            }
                        }

                        ExternalActionCard(
                            title = "חפש טריילרים ביוטיוב",
                            subtitle = "צפה בקטעים וסרטוני לימוד",
                            icon = Icons.Default.PlayCircle,
                            color = Color(0xFFFF0000)
                        ) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(item.title + " trailer", "UTF-8")}")))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExternalActionSmallCard(
                                title = "נטפליקס",
                                icon = Icons.Default.Tv,
                                color = Color(0xFFE50914),
                                modifier = Modifier.weight(1f)
                            ) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.netflix.com/search?q=${URLEncoder.encode(item.title, "UTF-8")}")))
                            }
                            
                            ExternalActionSmallCard(
                                title = "טורנטים",
                                icon = Icons.Default.Download,
                                color = Color(0xFF4B4B4B),
                                modifier = Modifier.weight(1f)
                            ) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(item.title + " srt torrent", "UTF-8")}")))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalActionSmallCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
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
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.friends), // Use a default background as fallback
                placeholder = painterResource(R.drawable.friends)
            )
            
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
