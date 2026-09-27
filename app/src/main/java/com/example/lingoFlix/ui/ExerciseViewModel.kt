package com.example.lingoFlix.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.lingoFlix.model.Question
import com.example.lingoFlix.model.SubtitleSegment
import com.example.lingoFlix.util.GameLogic
import java.io.File
import com.example.lingoFlix.utils.SrtParser
import android.net.Uri

class ExerciseViewModel : ViewModel() {

    private val _currentQuestion = mutableStateOf<Question?>(null)
    val currentQuestion: State<Question?> = _currentQuestion

    private val _score = mutableStateOf(0)
    val score: State<Int> = _score

    private val _hearts = mutableStateOf(3)
    val hearts: State<Int> = _hearts

    private val _questionIndex = mutableStateOf(0)
    val questionIndex: State<Int> = _questionIndex

    private val _isGameOver = mutableStateOf(false)
    val isGameOver: State<Boolean> = _isGameOver

    private val _streak = mutableIntStateOf(0)
    val streak: State<Int> = _streak

    private val _shuffledWords = mutableStateOf<List<String>>(emptyList())
    val shuffledWords: State<List<String>> = _shuffledWords
    
    var totalQuestions = 0
        private set

    private var allSegments = listOf<SubtitleSegment>()

    fun loadProject(srtFile: File) {
        allSegments = SrtParser.parseSrtFile(srtFile, Uri.EMPTY).mapIndexed { index, clip ->
            SubtitleSegment(index, clip.startTimeMs, clip.endTimeMs, clip.text)
        }
        totalQuestions = allSegments.size
        _questionIndex.value = 0
        _hearts.value = 3
        _streak.value = 0
        _isGameOver.value = false
        nextQuestion()
    }

    fun nextQuestion(difficulty: GameLogic.Difficulty = GameLogic.Difficulty.EASY) {
        if (_questionIndex.value < allSegments.size && _hearts.value > 0) {
            val segment = allSegments[_questionIndex.value]
            val difficultyStr = when (difficulty) {
                GameLogic.Difficulty.HARD -> "קשה"
                GameLogic.Difficulty.MEDIUM -> "בינוני"
                else -> "קל"
            }
            val quizData = GameLogic.prepareQuiz(segment.text, difficultyStr)
            val maskedText = quizData.words
                .mapIndexed { index, word -> if (index in quizData.hiddenIndices) "____" else word }
                .joinToString("")
            val targetWords = quizData.hiddenIndices.sorted().map { quizData.words[it] }
            val question = Question(
                fullText = segment.text,
                maskedText = maskedText,
                targetWords = targetWords
            )
            _currentQuestion.value = question
            _shuffledWords.value = question.fullText.split(" ").filter { it.isNotBlank() }.shuffled()
            _questionIndex.value++
        } else {
            _currentQuestion.value = null 
            _isGameOver.value = true
        }
    }

    fun submitAnswer(answer: String) {
        val current = _currentQuestion.value ?: return
        val isCorrect = answer.trim().lowercase() == current.fullText.trim().lowercase()
        
        if (isCorrect) {
            _score.value += 10 + (_streak.value * 2)
            _streak.value++
        } else {
            _hearts.value = (_hearts.value - 1).coerceAtLeast(0)
            _streak.value = 0
            if (_hearts.value == 0) {
                _isGameOver.value = true
            }
        }
    }
}
