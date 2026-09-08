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
import com.example.lingoFlix.utils.LingoLog
import coil.compose.AsyncImage
import com.example.lingoFlix.model.DynamicSkill
import java.io.File
import java.util.Calendar

@Composable
fun DashboardScreen(
    onMyVideos: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onRandomSentences: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onBattleMode: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onVideoSelected: (File) -> Unit = {},
    totalXP: Int = 0,
    currentStreak: Int = 0,
    userName: String = "Lingo Learner",
    skills: List<DynamicSkill> = emptyList(),
    onBuildSkill: () -> Unit = {},
    onAdminClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val xpInCurrentLevel = totalXP % 1000
    val userProgress = xpInCurrentLevel / 1000f

    Scaffold(
        containerColor = Color.Transparent
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
                        UserProfileSection(
                            userName = userName,
                            totalXP = totalXP,
                            onProfileClick = onProfileClick,
                            onAdminClick = onAdminClick
                        )
                    }

                    item {
                        // Daily Activities Section
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9600),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "האימון היומי שלך",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4B4B4B)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                // Featured "Start Game" Widget
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.horizontalGradient(listOf(Color(0xFFCE93D8), Color(0xFFBA68C8))))
                                        .clickable {
                                            try {
                                                LingoLog.i("DashboardScreen", "Starting random sentences game")
                                                onRandomSentences()
                                            } catch (e: Exception) {
                                                LingoLog.e("DashboardScreen", "Error starting random sentences", e)
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayCircleFilled, null, tint = Color.White, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("התחל משחק מרכזי", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                            Text("תרגול רנדומלי מכל הסרטונים שלך", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DuoButton(
                                        text = "המועדפים שלי",
                                        onClick = onFavorites,
                                        color = Color(0xFFFFD600),
                                        darkColor = Color(0xFFE6C300),
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = Icons.Default.Star
                                    )
                                    DuoButton(
                                        text = "מאגר הסרטים",
                                        onClick = onMyVideos,
                                        color = Color(0xFF1CB0F6),
                                        darkColor = Color(0xFF1899D6),
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = Icons.Default.VideoLibrary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Weekly Streak Section
                        WeeklyProgressSection(currentStreak)
                    }

                    if (skills.isNotEmpty()) {
                        item {
                            Text("הסקילים שלך", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4B4B4B))
                        }
                        items(skills) { skill ->
                            SkillCard(skill)
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
