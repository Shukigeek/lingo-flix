package com.example.lingoFlix.utils

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import java.io.File

object SubtitleGenerator {
    private const val TAG = "SubtitleGenerator"

    suspend fun generateSubtitles(
        context: Context,
        videoFile: File,
        userId: String,
        onProgress: (String) -> Unit
    ): Result<File> {
        val apiKey = SecurityUtils.getUserApiKey(context, userId)
        if (apiKey.isNullOrBlank()) {
            return Result.failure(Exception("נא להזין API KEY בהגדרות"))
        }

        return try {
            onProgress("מכין את הסרטון לעיבוד...")
            val videoBytes = videoFile.readBytes()
            
            // Check file size. Direct upload might fail if > 20MB for some tiers, 
            // but Gemini 1.5 handles large context. 
            // For a "pro" feel, we should warn if it's too big.
            if (videoBytes.size > 20 * 1024 * 1024) {
                Log.w(TAG, "Video file is large: ${videoBytes.size / 1024 / 1024}MB")
            }

            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )

            onProgress("מחלץ כתוביות בעזרת AI (זה עשוי לקחת זמן)...")
            
            val prompt = content {
                blob("video/mp4", videoBytes)
                text("""
                    Analyze this video and transcribe the speech into a valid SRT subtitle file.
                    - Use the original language of the video.
                    - Ensure the timestamps are accurate (Format: 00:00:00,000 --> 00:00:00,000).
                    - Output ONLY the raw SRT content. Do not include any explanations, markdown code blocks, or preamble.
                    - If the video has music only or no speech, return an empty string.
                """.trimIndent())
            }

            val response = generativeModel.generateContent(prompt)
            var srtContent = response.text?.trim() ?: ""

            // Clean up common AI formatting artifacts
            if (srtContent.startsWith("```")) {
                srtContent = srtContent.removeSurrounding("```srt", "```")
                srtContent = srtContent.removeSurrounding("```", "```")
                srtContent = srtContent.trim()
            }

            if (srtContent.isBlank()) {
                return Result.failure(Exception("ה-AI לא הצליח להפיק כתוביות (אולי אין דיבור בסרטון?)"))
            }

            // Simple validation: check if it contains --> 
            if (!srtContent.contains(" --> ")) {
                return Result.failure(Exception("התגובה מה-AI לא נראית כמו קובץ כתוביות תקין"))
            }

            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            srtFile.writeText(srtContent)
            
            Result.success(srtFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating subtitles", e)
            Result.failure(e)
        }
    }
}
