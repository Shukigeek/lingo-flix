package com.example.lingoFlix.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.ui.components.DuoGray
import com.example.lingoFlix.util.GameLogic

@Composable
fun DifficultyScreen(
    videoTitle: String,
    onDifficultySelected: (GameLogic.Difficulty) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "בחר רמת קושי",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4B4B4B)
        )
        Text(
            text = videoTitle,
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        DifficultyCard(
            level = "קל",
            description = "מילה אחת נסתרת",
            xp = "10 XP",
            icon = Icons.Default.ElectricBolt,
            color = Color(0xFF58CC02),
            onClick = { onDifficultySelected(GameLogic.Difficulty.EASY) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DifficultyCard(
            level = "בינוני",
            description = "חלק מהמילים נסתרות",
            xp = "20 XP",
            icon = Icons.Default.Star,
            color = Color(0xFFFFB800),
            onClick = { onDifficultySelected(GameLogic.Difficulty.MEDIUM) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DifficultyCard(
            level = "קשה",
            description = "רוב המשפט נסתר",
            xp = "50 XP",
            icon = Icons.Default.Psychology,
            color = Color(0xFFEA2B2B),
            onClick = { onDifficultySelected(GameLogic.Difficulty.HARD) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(onClick = onBack) {
            Text("ביטול", color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun DifficultyCard(
    level: String,
    description: String,
    xp: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, DuoGray),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(level, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4B4B4B))
                Text(description, fontSize = 14.sp, color = Color.Gray)
            }

            Text(xp, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 16.sp)
        }
    }
}
