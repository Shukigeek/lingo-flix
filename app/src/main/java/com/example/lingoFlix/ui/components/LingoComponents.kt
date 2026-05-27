package com.example.lingoFlix.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.alpha
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
fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
    onNext: () -> Unit,
    isVisible: Boolean = true,
    currentStreak: Int = 0
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Sound effect
    LaunchedEffect(isVisible) {
        if (isVisible) {
            val soundUri = if (isCorrect) {
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            } else {
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI // Fallback
            }
            try {
                val mediaPlayer = android.media.MediaPlayer.create(context, soundUri)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { it.release() }
            } catch (e: Exception) {}
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        val bgColor = if (isCorrect) Color(0xFFD7FFB8) else Color(0xFFFFDFE0)
        val textColor = if (isCorrect) DuoDarkGreen else DuoDarkRed
        val btnColor = if (isCorrect) DuoGreen else DuoRed
        val btnDarkColor = if (isCorrect) DuoDarkGreen else DuoDarkRed

        Box(modifier = Modifier.fillMaxWidth()) {
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
                    
                    if (isCorrect && currentStreak > 0 && currentStreak % 5 == 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔥 $currentStreak תשובות ברצף!",
                            color = Color(0xFFFF9600),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
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

            // Simple Confetti Effect
            if (isCorrect && currentStreak > 0 && currentStreak % 5 == 0) {
                ConfettiEffect()
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    val particles = remember { List(10) { (0..100).random() to (0..100).random() } }
    
    particles.forEach { (x, _) ->
        var offsetY by remember { mutableStateOf(0f) }
        var alpha by remember { mutableStateOf(1f) }
        
        val animatedOffsetY by androidx.compose.animation.core.animateFloatAsState(
            targetValue = -300f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
        )
        val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
        )
        
        LaunchedEffect(Unit) {
            offsetY = -300f
            alpha = 0f
        }

        Text(
            text = "🎉",
            modifier = Modifier
                .offset(x = (x * 3).dp, y = animatedOffsetY.dp)
                .alpha(animatedAlpha),
            fontSize = 24.sp
        )
    }
}

@Composable
fun WordTile(
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                enabled = !isSelected
            )
            .background(
                if (isSelected) DuoGray else Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                if (isSelected) DuoGray else DuoGray,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word,
            color = if (isSelected) Color.Transparent else Color(0xFF4B4B4B),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        if (!isSelected) {
            // Add a small shadow effect to tiles
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 2.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            )
        }
    }
}
