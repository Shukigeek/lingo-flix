package com.example.lingoFlix.utils

import android.net.Uri
import android.util.Log
import com.example.lingoFlix.model.SubtitleClip
import java.io.File

object SrtParser {
    fun parseSrtFile(srtFile: File, videoUri: Uri): List<SubtitleClip> {
        Log.d("SrtParser", "Parsing SRT file: ${srtFile.absolutePath}")
        val clips = mutableListOf<SubtitleClip>()
        try {
            if (!srtFile.exists()) {
                Log.e("SrtParser", "File does not exist!")
                return emptyList()
            }
            val bytes = srtFile.readBytes()
            val encoding = detectEncoding(bytes)
            Log.d("SrtParser", "Detected encoding: ${encoding.name()}")
            val content = String(bytes, encoding)
            
            val lines = content.lines().map { it.trim() }
            Log.d("SrtParser", "Total lines to process: ${lines.size}")
            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                if (line.contains(" --> ")) {
                    val times = line.split(" --> ")
                    if (times.size == 2) {
                        val startTime = parseSrtTime(times[0])
                        val endTime = parseSrtTime(times[1])
                        
                        val textLines = mutableListOf<String>()
                        i++
                        while (i < lines.size && lines[i].isNotEmpty() && !lines[i].contains(" --> ")) {
                            if (lines[i].toIntOrNull() == null) {
                                textLines.add(lines[i])
                            }
                            i++
                        }
                        val text = textLines.joinToString("\n").trim()
                        if (text.isNotEmpty()) {
                            clips.add(SubtitleClip(text, startTime, endTime, videoUri))
                        }
                        continue
                    }
                }
                i++
            }
            Log.d("SrtParser", "Successfully parsed ${clips.size} clips")
        } catch (e: Exception) {
            Log.e("SrtParser", "Error parsing SRT", e)
        }
        return clips
    }

    private fun detectEncoding(bytes: ByteArray): java.nio.charset.Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) return Charsets.UTF_8
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return Charsets.UTF_16BE
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return Charsets.UTF_16LE
        
        if (isUtf8(bytes)) return Charsets.UTF_8

        var hebrewChars = 0
        var spanishChars = 0
        
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i in 0xE0..0xFA) hebrewChars++
            if (i == 0xF1 || i == 0xD1 || i == 0xE1 || i == 0xE9 || i == 0xED || i == 0xF3 || i == 0xFA || i == 0xFC) {
                spanishChars++
            }
        }
        
        return when {
            hebrewChars > spanishChars && hebrewChars > 5 -> java.nio.charset.Charset.forName("windows-1255")
            spanishChars > 0 -> java.nio.charset.Charset.forName("windows-1252")
            else -> Charsets.UTF_8
        }
    }

    private fun isUtf8(bytes: ByteArray): Boolean {
        var i = 0
        var hasMultibyte = false
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b <= 0x7F) {
                i++
                continue
            }
            hasMultibyte = true
            val count = when {
                b in 0xC2..0xDF -> 1
                b in 0xE0..0xEF -> 2
                b in 0xF0..0xF4 -> 3
                else -> return false
            }
            if (i + count >= bytes.size) return false
            for (j in 1..count) {
                if ((bytes[i + j].toInt() and 0xC0) != 0x80) return false
            }
            i += count + 1
        }
        return hasMultibyte // If no multibyte found, it's ASCII (which is valid UTF-8)
    }

    private fun parseSrtTime(timeStr: String): Long {
        val parts = timeStr.replace(',', '.').split(":")
        if (parts.size != 3) return 0
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val secondsWithMs = parts[2].toDouble()
        return (hours * 3600000 + minutes * 60000 + (secondsWithMs * 1000).toLong())
    }

    fun detectSubtitleLanguage(file: File): String {
        return try {
            val bytes = file.readBytes()
            val encoding = detectEncoding(bytes)
            val content = String(bytes, encoding)
            
            if (content.any { it in '\u0590'..'\u05FF' }) return "עברית"
            if (content.any { it in '\u0400'..'\u04FF' }) return "רוסית"
            if (content.any { it in '\u0600'..'\u06FF' }) return "ערבית"
            if (content.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }) return "יפנית"
            if (content.any { it in '\u4E00'..'\u9FFF' }) return "סינית"
            if (content.any { it in '\uAC00'..'\uD7AF' }) return "קוריאנית"
            
            val spanishChars = setOf('ñ', 'á', 'é', 'í', 'ó', 'ú', 'ü', '¡', '¿', 'Ñ', 'Á', 'É', 'Í', 'Ó', 'Ú', 'Ü')
            if (content.any { it in spanishChars }) return "ספרדית"
            
            val frenchChars = setOf('à', 'â', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'î', 'ï', 'ô', 'œ', 'ù', 'û', 'ü', 'ÿ')
            if (content.any { it in frenchChars }) return "צרפתית"

            val germanChars = setOf('ä', 'ö', 'ü', 'ß', 'Ä', 'Ö', 'Ü')
            if (content.any { it in germanChars }) return "גרמנית"
            
            "אנגלית"
        } catch (e: Exception) {
            "לא ידוע"
        }
    }

    fun getLanguageFlag(language: String): String {
        return when (language) {
            "עברית" -> "🇮🇱"
            "אנגלית" -> "🇺🇸"
            "ספרדית" -> "🇪🇸"
            "רוסית" -> "🇷🇺"
            "ערבית" -> "🇸🇦"
            "צרפתית" -> "🇫🇷"
            "גרמנית" -> "🇩🇪"
            "איטלקית" -> "🇮🇹"
            "יפנית" -> "🇯🇵"
            "סינית" -> "🇨🇳"
            "קוריאנית" -> "🇰🇷"
            else -> "🏳️"
        }
    }
}
