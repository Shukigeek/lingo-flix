package com.example.lingoFlix.utils

import android.net.Uri
import com.example.lingoFlix.model.SubtitleClip
import java.io.File

object VideoPlayerLogic {
    fun detectLanguage(videoUri: Uri, clips: List<SubtitleClip>?, currentClipIndex: Int): String {
        // Priority 1: Check the current clip text if it's Hebrew
        val currentText = if (clips != null && currentClipIndex < clips.size) clips[currentClipIndex].text else ""
        if (currentText.any { it in '\u0590'..'\u05FF' }) return "עברית"
        
        // Priority 2: Check the SRT file
        val targetUri = if (clips != null && currentClipIndex < clips.size) clips[currentClipIndex].videoUri else videoUri
        if (targetUri.scheme == "file") {
            val videoFile = File(targetUri.path!!)
            val srtFile = FileUtils.findBestSrtForVideo(videoFile)
            if (srtFile != null && srtFile.exists()) {
                val lang = SrtParser.detectSubtitleLanguage(srtFile)
                LingoLog.d("VideoPlayerLogic", "Detected language for ${videoFile.name}: $lang")
                return lang
            } else return "אנגלית"
        } else return "אנגלית"
    }

    fun getPreferredAudioLang(detectedLanguage: String): String {
        return when (detectedLanguage) {
            "עברית" -> "he"
            "ספרדית" -> "es"
            "ערבית" -> "ar"
            "צרפתית" -> "fr"
            "גרמנית" -> "de"
            "איטלקית" -> "it"
            "פורטוגזית" -> "pt"
            "סינית" -> "zh"
            "יפנית" -> "ja"
            "קוריאנית" -> "ko"
            else -> "en"
        }
    }

    fun prepareQuiz(text: String, difficulty: String, quizType: String): Pair<Set<Int>, List<String>> {
        return try {
            val words = text.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;])|(?=[.,!?;])")).filter { it.isNotBlank() }
            val validIndices = words.indices.filter { words[it].length > 1 && words[it].any { c -> c.isLetter() } }
            
            var hiddenIndices = emptySet<Int>()
            if (validIndices.isNotEmpty()) {
                val countToHide = if (quizType == "multiple_choice") 1 else {
                    when (difficulty) {
                        "קל" -> 1
                        "בינוני" -> (validIndices.size * 0.4).toInt().coerceAtLeast(1)
                        "קשה" -> (validIndices.size * 0.7).toInt().coerceAtLeast(1)
                        else -> 1 // Default to easy
                    }
                }
                hiddenIndices = validIndices.shuffled().take(countToHide).toSet()
            }
            Pair(hiddenIndices, words)
        } catch (e: Exception) {
            LingoLog.e("VideoPlayerLogic", "Error preparing quiz", e)
            Pair(emptySet(), text.split(" "))
        }
    }

    fun getFontFamily(fontName: String): androidx.compose.ui.text.font.FontFamily {
        return when(fontName) {
            "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
            "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
            "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
            else -> androidx.compose.ui.text.font.FontFamily.SansSerif
        }
    }
}
