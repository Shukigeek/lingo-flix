package com.example.lingoFlix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.*
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
        videoDir.listFiles()?.filter { it.extension != "srt" }
            ?.sortedByDescending { it.lastModified() }
            ?.take(3) ?: emptyList()
    }
    
    val xpInCurrentLevel = totalXP % 1000
    val userProgress = xpInCurrentLevel / 1000f

    Scaffold(
        containerColor = Color.Transparent, // Make scaffold transparent to show background image
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = onFavorites,
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Favorites")
                }
                FloatingActionButton(
                    onClick = onUploadVideo,
                    containerColor = DuoGreen,
                    contentColor = Color.White
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
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
                ) {
                    item {
                        Surface(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "היי $userName!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1CB0F6),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item {
                        Surface(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "הסרטונים האחרונים",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B4B4B)
                                )
                            }
                        }
                    }

                    if (videoProjects.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DuoGray.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("אין סרטונים עדיין", color = Color.Gray)
                            }
                        }
                    } else {
                        items(videoProjects) { file ->
                            VideoCard(
                                file = file,
                                onClick = { onVideoSelected(file) }
                            )
                        }
                    }
                    
                    item {
                        DuoButton(
                            text = "למאגר הסרטונים שלי",
                            onClick = onMyVideos,
                            color = Color(0xFF1CB0F6), // DuoBlue
                            darkColor = Color(0xFF1899D6),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        DuoButton(
                            text = "משפטים רנדומליים",
                            onClick = onRandomSentences,
                            color = Color(0xFFCE93D8),
                            darkColor = Color(0xFFBA68C8),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "הסטטיסטיקה השבועית",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B4B4B),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatBox(
                                label = "נקודות XP",
                                value = totalXP.toString(),
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFC107),
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

@Composable
fun VideoCard(
    file: File,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(2.dp, DuoGray),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DuoBlue, DuoBlue.copy(alpha = 0.6f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF4B4B4B),
                    maxLines = 1
                )
            }
            
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = DuoGray, modifier = Modifier.size(20.dp))
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
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, DuoGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
