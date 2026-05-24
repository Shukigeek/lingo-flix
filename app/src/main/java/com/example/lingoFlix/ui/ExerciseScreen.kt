package com.example.lingoFlix.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    onBack: () -> Unit
) {
    val question by viewModel.currentQuestion
    var userAnswer by remember { mutableStateOf("") }
    var feedbackState by remember { mutableStateOf<Boolean?>(null) } // null = input, true = correct, false = wrong

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                ProgressBar(progress = 0.5f) // Placeholder progress
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
                    onNext = {
                        feedbackState = null
                        userAnswer = ""
                        viewModel.nextQuestion()
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

                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { if (feedbackState == null) userAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    placeholder = { Text("הקלד כאן...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = DuoBlue,
                        unfocusedIndicatorColor = DuoGray
                    )
                )
            } else {
                Text("כל הכבוד! סיימת את הפרק.", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
