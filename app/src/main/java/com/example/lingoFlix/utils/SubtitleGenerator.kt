package com.example.lingoFlix.utils

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object SubtitleGenerator {
    private const val TAG = "SubtitleGenerator"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun generateSubtitles(
        context: Context,
        videoFile: File,
        userId: String,
        onProgress: (String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        // Try OpenAI Whisper first (Higher Quality)
        val openAiKey = SecurityUtils.getOpenAiApiKey(context, userId)
        if (!openAiKey.isNullOrBlank()) {
            return@withContext generateWithWhisper(context, videoFile, openAiKey, onProgress)
        }

        // Fallback to Gemini (if no OpenAI key)
        val geminiKey = SecurityUtils.getUserApiKey(context, userId)
        if (!geminiKey.isNullOrBlank()) {
            return@withContext generateWithGemini(videoFile, geminiKey, onProgress)
        }

        Result.failure(Exception("נא להזין API KEY (OpenAI או Gemini) בהגדרות"))
    }

    private suspend fun generateWithWhisper(
        context: Context,
        videoFile: File,
        apiKey: String,
        onProgress: (String) -> Unit
    ): Result<File> {
        return try {
            onProgress("מחלץ אודיו מהסרטון...")
            val audioFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.m4a")
            val extractionSuccess = AudioUtils.extractAudioFromVideo(videoFile, audioFile)
            
            val targetFile = if (extractionSuccess && audioFile.exists()) audioFile else videoFile
            
            onProgress("מייצר כתוביות בעזרת Whisper Pro...")
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "srt")
                .addFormDataPart("file", targetFile.name, targetFile.asRequestBody("audio/mpeg".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("https://api.openai.org/v1/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val srtContent = response.body?.string() ?: ""

            if (!response.isSuccessful || srtContent.isBlank()) {
                val error = response.body?.string() ?: "שגיאה לא ידועה ב-Whisper"
                LingoLog.e(TAG, "Whisper API failed: $error")
                return Result.failure(Exception("ה-AI נכשל: $error"))
            }

            // Cleanup temp file
            if (targetFile != videoFile) targetFile.delete()

            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            srtFile.writeText(srtContent)
            
            Result.success(srtFile)
        } catch (e: Exception) {
            LingoLog.e(TAG, "Error in Whisper generation", e)
            Result.failure(e)
        }
    }

    private suspend fun generateWithGemini(
        videoFile: File,
        apiKey: String,
        onProgress: (String) -> Unit
    ): Result<File> {
        return try {
            onProgress("מייצר כתוביות בעזרת Gemini (בסיסי)...")
            val videoBytes = videoFile.readBytes()

            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )

            val prompt = content {
                blob("video/mp4", videoBytes)
                text("""
                    Analyze this video and transcribe the speech into a valid SRT subtitle file.
                    - Use the original language of the video.
                    - Ensure the timestamps are accurate.
                    - Output ONLY the raw SRT content.
                """.trimIndent())
            }

            val response = generativeModel.generateContent(prompt)
            var srtContent = response.text?.trim() ?: ""

            if (srtContent.startsWith("```")) {
                srtContent = srtContent.removeSurrounding("```srt", "```")
                srtContent = srtContent.removeSurrounding("```", "```")
                srtContent = srtContent.trim()
            }

            if (!srtContent.contains(" --> ")) {
                return Result.failure(Exception("התגובה מה-Gemini לא תקינה"))
            }

            val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
            srtFile.writeText(srtContent)
            
            Result.success(srtFile)
        } catch (e: Exception) {
            LingoLog.e(TAG, "Error in Gemini generation", e)
            Result.failure(e)
        }
    }
}
