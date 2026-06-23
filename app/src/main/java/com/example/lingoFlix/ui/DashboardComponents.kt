package com.example.lingoFlix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.utils.LingoLog
import java.io.File
import java.util.Calendar

@Composable
fun UserProfileSection(
    userName: String,
    totalXP: Int,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    val xpInCurrentLevel = totalXP % 1000
    val userProgress = xpInCurrentLevel / 1000f

    Surface(
        color = Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                LingoLog.d("UserProfileSection", "Profile clicked")
                onProfileClick()
            }) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFF1CB0F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userName.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "היי $userName! 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4B4B4B)
                    )
                    Text("מוכן לתרגול היומי?", color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    LingoLog.i("UserProfileSection", "Admin panel requested")
                    onAdminClick()
                }) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = Color(0xFF1CB0F6))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
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

@Composable
fun WeeklyProgressSection(streak: Int) {
    Surface(
        color = Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "התקדמות שבועית",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4B4B4B)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 // 0-based index
                
                days.forEachIndexed { index, day ->
                    val isDone = index < today // Mock logic: assume previous days were done if streak is high
                    DayCircle(day, isDone || (index == today && streak > 0))
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
            .clickable {
                try {
                    LingoLog.d("VideoCard", "Clicked on video: ${file.name}")
                    onClick()
                } catch (e: Exception) {
                    LingoLog.e("VideoCard", "Error handling video click", e)
                }
            },
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
