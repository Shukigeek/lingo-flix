package com.example.lingoFlix.ui.discovery

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lingoFlix.R
import com.example.lingoFlix.model.SearchResult
import com.example.lingoFlix.ui.components.YoutubePlayer
import com.example.lingoFlix.utils.LingoLog
import java.net.URLEncoder

@Composable
fun DiscoveryDetailContent(
    item: SearchResult, 
    onDismiss: () -> Unit,
    onAddToRecommendations: (SearchResult) -> Unit
) {
    val context = LocalContext.current
    val className = "DiscoveryDetailContent"

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
                    model = item.imageUrl ?: "",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.friends),
                    placeholder = painterResource(R.drawable.friends)
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
                onClick = onDismiss,
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
                fontWeight = FontWeight.Black,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            try {
                                onAddToRecommendations(item)
                                Toast.makeText(context, "נוסף למומלצי המערכת!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                LingoLog.e(className, "Failed to add recommendation", e)
                            }
                        }
                    )
                }
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
                subtitle = if (item.customTelegramLink != null) "מעבר לערוץ הרשמי" else "חיפוש ישיר עבור ${item.title}",
                icon = Icons.AutoMirrored.Filled.Send,
                color = Color(0xFF24A1DE)
            ) {
                try {
                    if (item.customTelegramLink != null) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.customTelegramLink)))
                    } else {
                        val encodedQuery = URLEncoder.encode(item.title, "UTF-8")
                        val telegramUri = Uri.parse("tg://search?text=$encodedQuery")
                        try { 
                            context.startActivity(Intent(Intent.ACTION_VIEW, telegramUri)) 
                        } catch (e: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/s/search?q=$encodedQuery")))
                        }
                    }
                } catch (e: Exception) {
                    LingoLog.e(className, "Failed to launch Telegram", e)
                    Toast.makeText(context, "שגיאה בפתיחת טלגרם", Toast.LENGTH_SHORT).show()
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
