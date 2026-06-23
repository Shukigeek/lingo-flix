package com.example.lingoFlix.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(
    onBack: () -> Unit,
    onScoreUpdate: (Int) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var isGameOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Mock Battle Question
    var currentQuestion by remember { mutableStateOf("Apple") }
    var options by remember { mutableStateOf(listOf("תפוח", "בננה", "תפוז", "אפרסק")) }
    var correctAnswer by remember { mutableStateOf("תפוח") }

    fun nextQuestion() {
        try {
            val questions = listOf(
                "Apple" to listOf("תפוח", "בננה", "תפוז", "אפרסק"),
                "Book" to listOf("ספר", "מחברת", "עט", "שולחן"),
                "Water" to listOf("מים", "מיץ", "חלב", "קפה"),
                "Friend" to listOf("חבר", "אח", "מורה", "שכן")
            )
            val next = questions.random()
            currentQuestion = next.first
            options = next.second.shuffled()
            correctAnswer = next.second[0]
        } catch (e: Exception) {
            LingoLog.e("BattleScreen", "Error generating next question", e)
        }
    }

    LaunchedEffect(Unit) {
        try {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            isGameOver = true
            onScoreUpdate(score)
        } catch (e: Exception) {
            LingoLog.e("BattleScreen", "Timer error", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("קרב קלפים!") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer & Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = if (timeLeft < 10) Color.Red else Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeLeft.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeft < 10) Color.Red else Color.White
                        )
                    }
                    
                    Surface(color = Color(0xFFFFD600), shape = RoundedCornerShape(16.dp)) {
                        Text(
                            text = "ציון: $score",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))

                // Question Card
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentQuestion,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1CB0F6)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Options
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    options.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { option ->
                                BattleOptionButton(
                                    text = option,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (option == correctAnswer) {
                                            score += 10
                                            nextQuestion()
                                        } else {
                                            score = (score - 5).coerceAtLeast(0)
                                            // Trigger shake animation or sound
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isGameOver) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("המשחק נגמר!", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("צברת $score נקודות!", color = Color(0xFFFFD600), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(0.6f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58CC02))
                        ) {
                            Text("חזור לתפריט", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattleOptionButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
