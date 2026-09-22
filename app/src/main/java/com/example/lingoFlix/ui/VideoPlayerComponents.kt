package com.example.lingoFlix.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.ui.components.DuoButton
import com.example.lingoFlix.ui.components.DuoGreen
import com.example.lingoFlix.ui.components.DuoDarkGreen
import com.example.lingoFlix.ui.components.DuoBlue

@Composable
fun QuizHud(
    heartsLeft: Int,
    comboCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(10) { index ->
                val isLost = index >= heartsLeft
                Icon(
                    imageVector = if (isLost) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (isLost) Color.Gray else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }

        if (comboCount >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color(0xFFFF9600),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "🔥 x$comboCount COMBO!",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CompletionOverlay(
    correctCount: Int,
    totalAttempted: Int,
    difficulty: String,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎬 סיימת את האימון!", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("ענית נכון על $correctCount מתוך $totalAttempted משפטים", color = Color.White.copy(alpha = 0.8f))
            
            val xp = correctCount * when(difficulty) { "קשה" -> 40; "בינוני" -> 30; else -> 20 }
            Text("+$xp XP", color = Color(0xFFFFD600), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(40.dp))
            DuoButton(text = "שחק שוב", onClick = onRestart, color = DuoGreen, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            DuoButton(text = "חזור", onClick = onBack, color = DuoBlue, modifier = Modifier.fillMaxWidth())
        }
    }
}
