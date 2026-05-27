package com.example.lingoFlix.utils

import android.net.Uri
import android.util.Log
import com.example.lingoFlix.model.SubtitleClip
import java.io.File

object SrtParser {
    fun parseSrtFile(srtFile: File, videoUri: Uri): List<SubtitleClip> {
        val clips = mutableListOf<SubtitleClip>()
        try {
            val bytes = srtFile.readBytes()
            val encoding = detectEncoding(bytes)
            val content = String(bytes, encoding)
            
            val lines = content.lines().map { it.trim() }
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
        } catch (e: Exception) {
            Log.e("SrtParser", "Error parsing SRT", e)
        }
        return clips
    }

    private fun detectEncoding(bytes: ByteArray): java.nio.charset.Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) return Charsets.UTF_8
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return Charsets.UTF_16BE
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return Charsets.UTF_16LE
        
        var hebrewChars = 0
        var spanishChars = 0
        var utf8Sequences = 0
        
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            // Hebrew range in Windows-1255
            if (b in 0xE0..0xFA) hebrewChars++
            // Common Spanish characters in ISO-8859-1 / Windows-1252
            if (b == 0xF1 || b == 0xD1 || b == 0xE1 || b == 0xE9 || b == 0xED || b == 0xF3 || b == 0xFA || b == 0xFC || b == 0xBF || b == 0xA1 || 
                b == 0xC1 || b == 0xC9 || b == 0xCD || b == 0xD3 || b == 0xDA || b == 0xDC) {
                spanishChars++
            }
            if (b in 0xC2..0xDF) {
                if (i + 1 < bytes.size && (bytes[i+1].toInt() and 0xFF) in 0x80..0xBF) {
                    utf8Sequences++
                    i++
                }
            } else if (b in 0xE0..0xEF) {
                if (i + 2 < bytes.size && (bytes[i+1].toInt() and 0xFF) in 0x80..0xBF && (bytes[i+2].toInt() and 0xFF) in 0x80..0xBF) {
                    utf8Sequences++
                    i += 2
                }
            }
            i++
        }
        
        return when {
            utf8Sequences > 0 -> Charsets.UTF_8
            hebrewChars > spanishChars && hebrewChars > 5 -> java.nio.charset.Charset.forName("windows-1255")
            spanishChars > 0 -> java.nio.charset.Charset.forName("windows-1252") // Spanish/Western
            else -> Charsets.UTF_8
        }
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
