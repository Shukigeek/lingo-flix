package com.example.lingoFlix.util

import com.example.lingoFlix.model.SubtitleClip
import com.example.lingoFlix.utils.LingoLog
import kotlin.random.Random

object GameLogic {

    enum class Difficulty {
        EASY,   // Mask 1 word
        MEDIUM, // Mask 40% of words
        HARD    // Mask 70% of words
    }

    data class QuizData(
        val hiddenIndices: Set<Int>,
        val words: List<String>
    )

    fun prepareQuiz(text: String, difficultyStr: String): QuizData {
        if (text.isBlank()) return QuizData(emptySet(), emptyList())
        
        return try {
            val words = text.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;])|(?=[.,!?;])")).filter { it.isNotBlank() }
            val validIndices = words.indices.filter { words[it].length > 1 && words[it].any { c -> c.isLetter() } }
            
            val difficulty = when(difficultyStr) {
                "קשה" -> Difficulty.HARD
                "בינוני" -> Difficulty.MEDIUM
                else -> Difficulty.EASY
            }

            var hiddenIndices = emptySet<Int>()
            if (validIndices.isNotEmpty()) {
                val countToHide = when (difficulty) {
                    Difficulty.EASY -> 1
                    Difficulty.MEDIUM -> (validIndices.size * 0.4).toInt().coerceAtLeast(1)
                    Difficulty.HARD -> (validIndices.size * 0.7).toInt().coerceAtLeast(1)
                }
                hiddenIndices = validIndices.shuffled().take(countToHide).toSet()
            }
            QuizData(hiddenIndices, words)
        } catch (e: Exception) {
            LingoLog.e("GameLogic", "Error preparing quiz", e)
            QuizData(emptySet(), text.split(" "))
        }
    }
    
    fun checkAnswer(userInput: String, words: List<String>, hiddenIndices: Set<Int>): Boolean {
        if (hiddenIndices.isEmpty()) return true
        
        val userWords = userInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val targetWords = hiddenIndices.map { words[it].filter { c -> c.isLetterOrDigit() }.lowercase() }
        
        if (userWords.size != targetWords.size) return false
        
        return targetWords.indices.all { i ->
            userWords[i].filter { c -> c.isLetterOrDigit() }.lowercase() == targetWords[i]
        }
    }
}
