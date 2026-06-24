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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lingoFlix.model.DynamicSkill
import com.example.lingoFlix.utils.LingoLog

@Composable
fun DayCircle(day: String, isDone: Boolean) {
    try {
        LingoLog.d("DashboardMiscComponents", "Rendering DayCircle for $day")
    } catch (e: Exception) {
        // Silently fail for logging
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDone) Color(0xFFFF9600) else Color(0xFFE5E5E5)),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF777777))
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
            contentScale = ContentScale.Crop,
            onError = { LingoLog.e("DashboardMiscComponents", "Failed to load image: $imageUrl") }
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

@Composable
fun SkillCard(skill: DynamicSkill) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F7F7),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE5E5E5))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF58CC02).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixNormal,
                    contentDescription = null,
                    tint = Color(0xFF58CC02)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4B4B4B))
                Text(skill.description, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    icon: ImageVector,
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
