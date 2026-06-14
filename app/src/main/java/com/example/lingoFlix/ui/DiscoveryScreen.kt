package com.example.lingoFlix.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val overview: String,
    val mediaType: String
)

@Composable
fun DiscoveryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<SearchResult?>(null) }

    // Using a public-ish TMDB search without key for demo or a placeholder 
    // In a real app, user should provide API key in settings
    fun search(searchQuery: String) {
        if (searchQuery.isBlank()) return
        isLoading = true
        scope.launch {
            try {
                // For demonstration, we'll use a public search or common shows if no key
                // Ideally, we'd use TMDB API here.
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                // Using a mock search logic for now to show how it would look
                // In a real implementation, we'd call an API.
                results = listOf(
                    SearchResult(1, "Friends", "https://image.tmdb.org/t/p/w500/fob2vYm5998Ar936IIn9C9YpXcr.jpg", "The lives, loves, and laughs of six young friends living in Manhattan.", "tv"),
                    SearchResult(2, "Breaking Bad", "https://image.tmdb.org/t/p/w500/ggm8bbub63OwoE1Z977jZ0hzR3H.jpg", "A high school chemistry teacher turned meth kingpin.", "tv"),
                    SearchResult(3, "The Office", "https://image.tmdb.org/t/p/w500/q979SsbMqUnwh7ZzQm07p5vU4lc.jpg", "The everyday lives of office employees in Scranton, Pennsylvania.", "tv"),
                    SearchResult(4, "Stranger Things", "https://image.tmdb.org/t/p/w500/49Wfivq1TdZ0VE6US7zAhS6FD9w.jpg", "When a young boy vanishes, a small town uncovers a mystery involving secret experiments.", "tv")
                ).filter { it.title.contains(searchQuery, ignoreCase = true) }
            } catch (e: Exception) {
                // handle error
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "גלה תוכן חדש",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                if (it.length > 2) search(it)
            },
            placeholder = { Text("חפש סדרה או סרט...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (results.isEmpty() && query.length > 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("לא נמצאו תוצאות ל-\"$query\"", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(results) { item ->
                    DiscoveryCard(item) { selectedItem = item }
                }
            }
        }
    }

    if (selectedItem != null) {
        val item = selectedItem!!
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = { Text(item.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AsyncImage(
                        model = item.posterPath,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(item.overview, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val telegramSearchQuery = URLEncoder.encode("${item.title} s01e01", "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://search?text=$telegramSearchQuery"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to web search if telegram app not installed
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/s/search?q=$telegramSearchQuery"))
                            context.startActivity(webIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("חפש פרקים בטלגרם")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItem = null }) { Text("סגור") }
            }
        )
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
                model = item.posterPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
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
