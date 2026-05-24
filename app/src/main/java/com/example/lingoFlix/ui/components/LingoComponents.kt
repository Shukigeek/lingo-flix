package com.example.lingoFlix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Duolingo Colors
val DuoGreen = Color(0xFF58CC02)
val DuoDarkGreen = Color(0xFF46A302)
val DuoRed = Color(0xFFEA2B2B)
val DuoDarkRed = Color(0xFFC32121)
val DuoBlue = Color(0xFF1CB0F6)
val DuoGray = Color(0xFFE5E5E5)

@Composable
fun DuoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DuoGreen,
    darkColor: Color = DuoDarkGreen,
    enabled: Boolean = true
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val verticalOffset = if (isPressed) 0.dp else 4.dp
    val shadowHeight = 4.dp

    Box(
        modifier = modifier
            .padding(bottom = shadowHeight) // Space for the shadow
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Shadow (Darker part)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = shadowHeight)
                .background(if (enabled) darkColor else Color(0xFFB7B7B7), RoundedCornerShape(16.dp))
        )
        // Main Button (Lighter part)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = verticalOffset)
                .background(if (enabled) color else DuoGray, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(DuoGray, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(DuoGreen, RoundedCornerShape(8.dp))
                .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun FeedbackBanner(
    isCorrect: Boolean,
    correctText: String,
    onNext: () -> Unit
) {
    val bgColor = if (isCorrect) Color(0xFFD7FFB8) else Color(0xFFFFDFE0)
    val textColor = if (isCorrect) DuoDarkGreen else DuoDarkRed
    val btnColor = if (isCorrect) DuoGreen else DuoRed
    val btnDarkColor = if (isCorrect) DuoDarkGreen else DuoDarkRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isCorrect) "מצוין!" else "לא בדיוק...",
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }
        
        if (!isCorrect) {
            Text(
                text = "התשובה הנכונה:",
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = correctText,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DuoButton(
            text = "המשך",
            onClick = onNext,
            color = btnColor,
            darkColor = btnDarkColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
