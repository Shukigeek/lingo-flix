package com.example.lingoFlix.ui

import android.net.Uri
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import com.example.lingoFlix.model.SubtitleClip
import java.io.File

@Composable
fun SubtitleSection(
    text: String,
    isQuizMode: Boolean,
    hiddenIndices: Set<Int>,
    wordsList: List<String>,
    userInput: String,
    isChecked: Boolean,
    detectedLanguage: String,
    subtitleColorHex: String,
    subtitleFontSize: Float,
    subtitleIsBold: Boolean,
    currentFontFamily: FontFamily,
    shakeOffset: Float,
    flashColor: Color
) {
    val isRtl = detectedLanguage == "עברית"
    val textColor = try { Color(android.graphics.Color.parseColor(subtitleColorHex)) } catch(e: Exception) { Color.White }
    
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val borderColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = MaterialTheme.colorScheme.tertiary,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderColor"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(bottom = 32.dp)
            .offset(x = shakeOffset.dp)
            .border(
                2.dp, 
                if (flashColor != Color.Transparent) flashColor else borderColor.copy(alpha = 0.3f), 
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.background(flashColor.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                ) {
                    if (isQuizMode && hiddenIndices.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            wordsList.forEachIndexed { index, word ->
                                if (hiddenIndices.contains(index)) {
                                    val isWordCorrect = userInput.split(Regex("\\s+")).any { it.trim().equals(word.trim(), ignoreCase = true) }
                                    Text(
                                        text = if (isChecked) word else "____",
                                        color = if (!isChecked) Color.Yellow else if (isWordCorrect) Color.Green else Color.Red,
                                        fontSize = subtitleFontSize.sp,
                                        fontWeight = if (subtitleIsBold) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = currentFontFamily,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                            textAlign = TextAlign.Center,
                                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = word,
                                        color = textColor,
                                        fontSize = subtitleFontSize.sp,
                                        fontWeight = if (subtitleIsBold) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = currentFontFamily,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                            textAlign = TextAlign.Center,
                                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = subtitleFontSize.sp,
                            fontWeight = if (subtitleIsBold) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = currentFontFamily,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizInputSection(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onCheck: () -> Unit,
    isChecked: Boolean,
    allCorrect: Boolean,
    hiddenWords: String
) {
    if (!isChecked) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = onUserInputChange,
                placeholder = { Text("הקלד את המילים החסרות...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onCheck) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Check")
                    }
                }
            )
        }
    } else {
        Text(
            text = if (allCorrect) "כל הכבוד! ✨" else "המילים היו: $hiddenWords",
            color = if (allCorrect) Color.Green else Color.Red,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
