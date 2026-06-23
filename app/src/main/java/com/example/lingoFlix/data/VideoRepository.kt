package com.example.lingoFlix.data

import android.content.Context
import android.net.Uri
import com.example.lingoFlix.model.VideoProject
import com.example.lingoFlix.utils.LingoLog
import java.io.File

class VideoRepository(private val context: Context) {

    private val subtitlesService = SubtitlesService()

    // התיקייה הייעודית של האפליקציה
    // getExternalFilesDir = תיקייה שרק האפליקציה שלך יכולה לגשת אליה
    private val baseDir: File
        get() = File(context.getExternalFilesDir(null), "LingoFlick").also {
            if (!it.exists()) {
                val created = it.mkdirs()
                LingoLog.d("VideoRepository", "Base directory created: $created")
            }
        }

    // ── שמירת וידאו ──────────────────────────────────────────
    fun saveVideo(uri: Uri): VideoProject {
        try {
            // שלב 1: קח את שם הקובץ המקורי מה-Uri
            val originalName = getFileName(uri) // למשל: "breaking_bad_s01e01.mp4"
            val nameWithoutExt = originalName.substringBeforeLast(".") // "breaking_bad_s01e01"

            // שלב 2: הגדר נתיבי יעד
            val videoFile = File(baseDir, originalName)
            val srtFile   = File(baseDir, "$nameWithoutExt.srt")

            // שלב 3: העתק את הוידאו
            // context.contentResolver פותח Uri כ-stream של bytes
            context.contentResolver.openInputStream(uri)?.use { input ->
                videoFile.outputStream().use { output ->
                    input.copyTo(output) // מעתיק byte אחרי byte
                }
            }

            // שלב 4: נסה להוריד כתוביות או צור קובץ SRT ריק
            if (!srtFile.exists()) {
                try {
                    val downloaded = subtitlesService.downloadSubtitles(nameWithoutExt, srtFile)
                    if (!downloaded) {
                        srtFile.createNewFile()
                        // כאן אפשר להוסיף קריאה ל-generateSubtitlesLocally בעתיד
                    }
                } catch (e: Exception) {
                    LingoLog.e("VideoRepository", "Error downloading/creating subtitles", e)
                    srtFile.createNewFile()
                }
            }

            return VideoProject(
                name      = nameWithoutExt,
                videoPath = videoFile.absolutePath,
                srtPath   = srtFile.absolutePath
            )
        } catch (e: Exception) {
            LingoLog.e("VideoRepository", "Failed to save video from URI: $uri", e)
            throw e
        }
    }

    // ── טעינת כל הפרויקטים הקיימים ───────────────────────────
    fun loadProjects(): List<VideoProject> {
        return try {
            // סרוק את התיקייה, מצא כל קובץ SRT, בנה VideoProject
            baseDir.listFiles()
                ?.filter { it.extension == "srt" }
                ?.map { srtFile ->
                    val nameWithoutExt = srtFile.nameWithoutExtension
                    VideoProject(
                        name      = nameWithoutExt,
                        videoPath = File(baseDir, "$nameWithoutExt.mp4").absolutePath,
                        srtPath   = srtFile.absolutePath
                    )
                } ?: emptyList()
        } catch (e: Exception) {
            LingoLog.e("VideoRepository", "Failed to load projects", e)
            emptyList()
        }
    }

    // ── עזר: שם קובץ מ-Uri ───────────────────────────────────
    private fun getFileName(uri: Uri): String {
        return try {
            // contentResolver.query שואל את Android: מה שם הקובץ הזה?
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && it.moveToFirst()) {
                    it.getString(nameIndex)
                } else {
                    "video_${System.currentTimeMillis()}.mp4"
                }
            } ?: "video_${System.currentTimeMillis()}.mp4"
        } catch (e: Exception) {
            LingoLog.e("VideoRepository", "Error getting file name from URI", e)
            "video_${System.currentTimeMillis()}.mp4"
        }
    }
}
