package com.example.lingoFlix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.R
import com.example.lingoFlix.ui.components.*

@Composable
fun DashboardScreen(
    onMyVideos: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onRandomSentences: () -> Unit = {},
    onFavorites: () -> Unit = {},
    totalXP: Int = 0,
    currentStreak: Int = 0,
    userName: String = "Lingo Learner"
) {
    // Determine progress based on XP (e.g., 5000 XP per level)
    val xpInCurrentLevel = totalXP % 1000
    val userProgress = xpInCurrentLevel / 1000f

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Background Image (Friends TV Show) ---
        // Note: Make sure friends_bg exists in your drawable folder.
        // I've uncommented this so it actually tries to load the image.
        Image(
            painter = painterResource(id = R.drawable.friends),
            contentDescription = "Friends Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f // Fade it a bit so buttons stand out
        )
        
        // Gradient Overlay to ensure text readability on top of the image
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

        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top Progress Bar ---
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ההתקדמות שלך",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(userProgress * 100).toInt()}%",
                            color = DuoGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(progress = userProgress)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
            ) {
                item {
                    Text(
                        text = "היי $userName!\nמוכן ללמוד?",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 38.sp
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // --- Button to view My Videos ---
                    DuoButton(
                        text = "הסרטונים שלי",
                        onClick = onMyVideos,
                        color = DuoBlue,
                        darkColor = DuoBlue.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Button to upload a video ---
                    DuoButton(
                        text = "העלאת סרטון",
                        onClick = onUploadVideo,
                        color = DuoGreen, // You can choose a different color if you like
                        darkColor = DuoDarkGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- Button to choose Random Sentences ---
                    DuoButton(
                        text = "משפטים רנדומליים",
                        onClick = onRandomSentences,
                        color = DuoGreen,
                        darkColor = DuoDarkGreen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Button to view Favorites ---
                    DuoButton(
                        text = "משפטים מועדפים ⭐",
                        onClick = onFavorites,
                        color = Color(0xFFFFC107), // Gold
                        darkColor = Color(0xFFFFA000),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "הסטטיסטיקה השבועית",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatBoxTransparent(
                            label = "נקודות XP",
                            value = totalXP.toString(),
                            icon = Icons.Default.Star,
                            color = Color(0xFFFFC107),
                            modifier = Modifier.weight(1f)
                        )
                        StatBoxTransparent(
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

@Composable
fun StatBoxTransparent(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
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
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
