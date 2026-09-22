package com.example.lingoFlix.utils

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.lingoFlix.utils.LingoLog
import java.io.File
import java.nio.ByteBuffer

object SubtitleExtractor {
    private const val TAG = "SubtitleExtractor"

    fun extractEmbeddedSubtitles(context: Context, videoFile: File): Result<File> {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(videoFile.absolutePath)
            var subtitleTrackIndex = -1
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("text/") || mime.contains("subtitle")) {
                    subtitleTrackIndex = i
                    break
                }
            }

            if (subtitleTrackIndex == -1) {
                return Result.failure(Exception("לא נמצאו כתוביות מובנות בסרטון זה"))
            }

            extractor.selectTrack(subtitleTrackIndex)
            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val srtContent = StringBuilder()
            var counter = 1

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val timeUs = extractor.sampleTime
                val bytes = ByteArray(sampleSize)
                buffer.get(bytes)
                val text = String(bytes).trim()

                if (text.isNotBlank()) {
                    val startTime = formatTime(timeUs)
                    // We don't know the duration of the sample easily without next sample
                    // but for basic extraction we can estimate or just use it.
                    // This is a simplified SRT generator.
                    srtContent.append("$counter\n")
                    srtContent.append("$startTime --> ${formatTime(timeUs + 3000000)}\n") // +3 seconds estimate
                    srtContent.append("$text\n\n")
                    counter++
                }

                buffer.clear()
                extractor.advance()
            }

            if (srtContent.isEmpty()) {
                return Result.failure(Exception("לא הצלחנו לחלץ טקסט מרצועת הכתוביות"))
            }

            srtFile.writeText(srtContent.toString())
            Result.success(srtFile)
        } catch (e: Exception) {
            LingoLog.e(TAG, "Error extracting subtitles", e)
            Result.failure(e)
        } finally {
            extractor.release()
        }
    }

    private fun formatTime(us: Long): String {
        val ms = us / 1000
        val hours = ms / 3600000
        val mins = (ms % 3600000) / 60000
        val secs = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }
}
