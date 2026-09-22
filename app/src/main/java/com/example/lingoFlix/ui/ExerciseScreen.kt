package com.example.lingoFlix.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lingoFlix.ui.components.*

@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = viewModel(),
    onBack: (gameOver: Boolean) -> Unit
) {
    val question by viewModel.currentQuestion
    val hearts by viewModel.hearts
    val questionIndex by viewModel.questionIndex
    val totalQuestions = viewModel.totalQuestions
    val isGameOver by viewModel.isGameOver
    val shuffledWords by viewModel.shuffledWords
    
    val selectedWords = remember { mutableStateListOf<Pair<Int, String>>() }
    val userAnswer = selectedWords.joinToString(" ") { it.second }
    var feedbackState by remember { mutableStateOf<Boolean?>(null) } // null = input, true = correct, false = wrong

    LaunchedEffect(isGameOver) {
        if (isGameOver && hearts == 0) {
            onBack(true)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack(false) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
                
                ProgressBar(
                    progress = if (totalQuestions > 0) questionIndex.toFloat() / totalQuestions else 0f,
                    modifier = Modifier.weight(1f)
                )

                Row(modifier = Modifier.padding(start = 8.dp)) {
                    repeat(3) { index ->
                        Icon(
                            imageVector = if (index < hearts) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (index < hearts) Color.Red else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (feedbackState == null) {
                Box(modifier = Modifier.padding(16.dp)) {
                    DuoButton(
                        text = "בדיקה",
                        onClick = {
                            val isCorrect = userAnswer.trim().lowercase() == question?.fullText?.trim()?.lowercase()
                            feedbackState = isCorrect
                            if (isCorrect) {
                                viewModel.submitAnswer(userAnswer)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = userAnswer.isNotEmpty()
                    )
                }
            } else {
                FeedbackBanner(
                    isCorrect = feedbackState!!,
                    correctText = question?.fullText ?: "",
                    isVisible = feedbackState != null,
                    currentStreak = viewModel.streak.value,
                    onNext = {
                        feedbackState = null
                        selectedWords.clear()
                        if (!isGameOver) {
                            viewModel.nextQuestion()
                        } else {
                             onBack(false)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "כתוב את מה ששמעת",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B4B4B)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Video / Audio Placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = Color(0xFFF7F7F7),
                border = androidx.compose.foundation.BorderStroke(2.dp, DuoGray)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("כאן יופיע קטע הוידאו", color = Color.Gray)
                    // TODO: Integrate Media3 Player here
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (question != null) {
                Text(
                    text = question!!.maskedText,
                    fontSize = 20.sp,
                    color = Color(0xFF4B4B4B),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Selected Words Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .padding(vertical = 16.dp)
                        .border(1.dp, DuoGray, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        selectedWords.forEach { pair ->
                            WordTile(
                                word = pair.second,
                                onClick = {
                                    if (feedbackState == null) {
                                        selectedWords.remove(pair)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Available Word Tiles
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    shuffledWords.forEachIndexed { index, word ->
                        val isSelected = selectedWords.any { it.first == index }
                        WordTile(
                            word = word,
                            isSelected = isSelected,
                            onClick = {
                                if (feedbackState == null) {
                                    selectedWords.add(index to word)
                                }
                            }
                        )
                    }
                }
            } else {
                Text("כל הכבוד! סיימת את הפרק.", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
