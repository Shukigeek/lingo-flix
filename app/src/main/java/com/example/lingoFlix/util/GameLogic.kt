package com.example.lingoFlix.util

import com.example.lingoFlix.model.Question
import com.example.lingoFlix.model.SubtitleSegment
import kotlin.random.Random

object GameLogic {

    enum class Difficulty {
        EASY,   // Mask 30% of words
        MEDIUM, // Mask 60% of words
        HARD    // Mask 100% of words
    }

    fun generateQuestion(segment: SubtitleSegment, difficulty: Difficulty): Question {
        val words = segment.text.split(" ").toMutableList()
        val maskedWords = mutableListOf<String>()
        
        val maskProbability = when (difficulty) {
            Difficulty.EASY -> 0.3
            Difficulty.MEDIUM -> 0.6
            Difficulty.HARD -> 1.0
        }

        val resultWords = words.mapIndexed { index, word ->
            // Don't mask very short words (like "a", "y") unless it's HARD
            if (difficulty != Difficulty.HARD && word.length <= 2) {
                word
            } else if (Random.nextDouble() < maskProbability) {
                maskedWords.add(word.replace(Regex("[^a-zA-Z\u00C0-\u017F]"), "")) // Keep only letters for the target
                "____"
            } else {
                word
            }
        }

        return Question(
            fullText = segment.text,
            maskedText = resultWords.joinToString(" "),
            targetWords = maskedWords
        )
    }
    
    fun checkAnswer(userAnswer: String, originalText: String): Int {
        // Simple scoring: 0-100 based on similarity
        // This is a placeholder for a better fuzzy match
        val cleanUser = userAnswer.trim().lowercase()
        val cleanOriginal = originalText.trim().lowercase()
        
        if (cleanUser == cleanOriginal) return 100
        
        // Basic word-by-word comparison
        val userWords = cleanUser.split(" ")
        val originalWords = cleanOriginal.split(" ")
        
        var matches = 0
        originalWords.forEach { word ->
            if (userWords.contains(word)) matches++
        }
        
        return ((matches.toFloat() / originalWords.size) * 100).toInt()
    }
}
