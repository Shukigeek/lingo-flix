package com.example.lingoFlix.util

import com.example.lingoFlix.model.SubtitleSegment
import java.io.File

object SrtParser {
    fun parse(file: File): List<SubtitleSegment> {
        if (!file.exists()) return emptyList()
        
        val segments = mutableListOf<SubtitleSegment>()
        val lines = file.readLines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                i++
                continue
            }
            
            // 1. Index
            val index = line.toIntOrNull() ?: 0
            i++
            
            if (i >= lines.size) break
            
            // 2. Time: 00:00:20,000 --> 00:00:24,400
            val timeLine = lines[i]
            val times = timeLine.split(" --> ")
            if (times.size == 2) {
                val start = parseTime(times[0])
                val end = parseTime(times[1])
                i++
                
                // 3. Text (can be multiple lines until empty line)
                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                    textBuilder.append(lines[i].trim())
                    i++
                }
                
                segments.add(SubtitleSegment(index, start, end, textBuilder.toString()))
            } else {
                i++
            }
        }
        return segments
    }

    private fun parseTime(timeStr: String): Long {
        // Format: HH:mm:ss,SSS
        val parts = timeStr.trim().replace(',', '.').split(":")
        var hours = 0L
        var minutes = 0L
        var seconds = 0.0
        
        if (parts.size == 3) {
            hours = parts[0].toLong()
            minutes = parts[1].toLong()
            seconds = parts[2].toDouble()
        }
        
        return (hours * 3600 + minutes * 60 + seconds.toLong()) * 1000 + ((seconds % 1) * 1000).toLong()
    }
}
