package com.example.lingoFlix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.*
import coil.compose.AsyncImage
import java.io.File

@Composable
fun DashboardScreen(
    onMyVideos: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onRandomSentences: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onVideoSelected: (File) -> Unit = {},
    totalXP: Int = 0,
    currentStreak: Int = 0,
    userName: String = "Lingo Learner"
) {
    val context = LocalContext.current
    val videoDir = remember { File(context.filesDir, "videos") }
    val videoProjects = remember(videoDir) {
        videoDir.listFiles()?.filter { !it.isDirectory && it.extension != "srt" }
            ?.sortedByDescending { it.lastModified() }
            ?.take(3) ?: emptyList()
    }
    
    val xpInCurrentLevel = totalXP % 1000
    val userProgress = xpInCurrentLevel / 1000f

    Scaffold(
        containerColor = Color.Transparent, 
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onFavorites,
                    containerColor = Color(0xFFFFD600),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Favorites")
                }
                FloatingActionButton(
                    onClick = onUploadVideo,
                    containerColor = Color(0xFF58CC02), // DuoGreen
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                ) {
                    item {
                        // User Profile Section
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "היי $userName! 👋",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF4B4B4B)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "רמה ${totalXP / 1000 + 1}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF1CB0F6),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "$xpInCurrentLevel / 1000 XP",
                                        fontSize = 14.sp,
                                        color = Color(0xFF777777),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LinearProgressIndicator(
                                    progress = { userProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = Color(0xFF58CC02),
                                    trackColor = Color(0xFFE5E5E5),
                                )
                            }
                        }
                    }

                    item {
                        // Quick Actions Section
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF1CB0F6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "הסרטונים האחרונים",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4B4B4B)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                if (videoProjects.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .background(Color(0xFFF7F7F7), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.VideoLibrary,
                                                contentDescription = null,
                                                tint = Color(0xFFAFAFAF),
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "העלה סרטון כדי להתחיל!",
                                                color = Color(0xFF777777),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        videoProjects.forEach { file ->
                                            VideoCard(
                                                file = file,
                                                onClick = { onVideoSelected(file) }
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                DuoButton(
                                    text = "למאגר הסרטונים שלי",
                                    onClick = onMyVideos,
                                    color = Color(0xFF1CB0F6),
                                    darkColor = Color(0xFF1899D6),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                DuoButton(
                                    text = "משפטים רנדומליים",
                                    onClick = onRandomSentences,
                                    color = Color(0xFFCE93D8),
                                    darkColor = Color(0xFFBA68C8),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        // Recommendations Section
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF58CC02),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "מומלץ עבורך",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4B4B4B)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RecommendationItem("Seinfeld", "https://image.tmdb.org/t/p/w200/a3m79vB7Z9z7S6zKi6thpS686ln.jpg", Modifier.weight(1f))
                                    RecommendationItem("The Bear", "https://image.tmdb.org/t/p/w200/5NX98f73YQzR14CgYfI6nFvR6U9.jpg", Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        // Stats Section
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9600),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "הסטטיסטיקה שלך",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4B4B4B)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatBox(
                                        label = "נקודות XP",
                                        value = totalXP.toString(),
                                        icon = Icons.Default.Stars,
                                        color = Color(0xFFFFD600),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatBox(
                                        label = "רצף ימים",
                                        value = currentStreak.toString(),
                                        icon = Icons.Default.LocalFireDepartment,
                                        color = Color(0xFFFF9600),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    file: File,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F7F7),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE5E5E5)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1CB0F6).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow, 
                    contentDescription = null, 
                    tint = Color(0xFF1CB0F6), 
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = file.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF4B4B4B),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Default.ChevronLeft, 
                contentDescription = null, 
                tint = Color(0xFFAFAFAF), 
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F7F7),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE5E5E5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4B4B4B)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF777777),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun RecommendationItem(
    title: String,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4B4B4B),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
