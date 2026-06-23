package com.example.lingoFlix.model

import com.example.lingoFlix.utils.LingoLog

/**
 * Represent a single subtitle entry from an SRT file.
 */
data class SubtitleItem(
    val index: Int,
    val startTime: Long, // Start time in milliseconds
    val endTime: Long,   // End time in milliseconds
    val text: String     // The actual text/sentence
)

/**
 * Simple utility to parse SRT files.
 * Since you know Python, think of this as a "string split and regex" operation.
 */
object SrtParser {
    
    fun parse(srtContent: String): List<SubtitleItem> {
        val items = mutableListOf<SubtitleItem>()
        // Normalize line endings and split by double newline (blocks)
        val blocks = srtContent.replace("\r\n", "\n").split("\n\n")
        
        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size >= 3) {
                try {
                    val index = lines[0].trim().toInt()
                    val times = lines[1].split(" --> ")
                    if (times.size == 2) {
                        val startMs = timeToMs(times[0].trim())
                        val endMs = timeToMs(times[1].trim())
                        val text = lines.drop(2).joinToString(" ").trim()
                        
                        items.add(SubtitleItem(index, startMs, endMs, text))
                    }
                } catch (e: Exception) {
                    LingoLog.e("SrtParser", "Failed to parse subtitle block: $block", e)
                    // Skip malformed blocks
                    continue
                }
            }
        }
        return items
    }

    /**
     * Converts SRT time format "00:00:20,000" to milliseconds.
     */
    private fun timeToMs(timeStr: String): Long {
        // Format: HH:mm:ss,SSS
        val parts = timeStr.replace(",", ".").split(":")
        if (parts.size != 3) return 0L
        
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val secondsParts = parts[2].split(".")
        val seconds = secondsParts[0].toLong()
        val millis = if (secondsParts.size > 1) secondsParts[1].toLong() else 0L
        
        return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
    }
}
