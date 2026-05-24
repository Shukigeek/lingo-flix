package com.example.lingoFlix.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.lingoFlix.model.Question
import com.example.lingoFlix.model.SubtitleSegment
import com.example.lingoFlix.util.GameLogic
import java.io.File
import com.example.lingoFlix.util.SrtParser

class ExerciseViewModel : ViewModel() {

    private val _currentQuestion = mutableStateOf<Question?>(null)
    val currentQuestion: State<Question?> = _currentQuestion

    private val _score = mutableStateOf(0)
    val score: State<Int> = _score
    
    private var allSegments = listOf<SubtitleSegment>()
    private var currentIndex = 0

    fun loadProject(srtFile: File) {
        allSegments = SrtParser.parse(srtFile)
        currentIndex = 0
        nextQuestion()
    }

    fun nextQuestion(difficulty: GameLogic.Difficulty = GameLogic.Difficulty.EASY) {
        if (currentIndex < allSegments.size) {
            val segment = allSegments[currentIndex]
            _currentQuestion.value = GameLogic.generateQuestion(segment, difficulty)
            currentIndex++
        } else {
            _currentQuestion.value = null // Game over
        }
    }

    fun submitAnswer(answer: String) {
        val current = _currentQuestion.value ?: return
        val points = GameLogic.checkAnswer(answer, current.fullText)
        _score.value += points
        // Auto move to next or let UI decide
    }
}
