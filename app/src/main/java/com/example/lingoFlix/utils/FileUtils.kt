package com.example.lingoFlix.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

object FileUtils {
    fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) name = it.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error getting file name", e)
        }
        return name
    }

    fun saveVideoToInternalStorage(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val videoDir = File(context.filesDir, "videos")
            
            // Create a subfolder based on the video name (without extension)
            val baseName = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
            val targetDir = File(videoDir, baseName)
            if (!targetDir.exists()) targetDir.mkdirs()
            
            val targetFile = File(targetDir, fileName)
            val outputStream = FileOutputStream(targetFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            Log.e("FileUtils", "Error saving video", e)
            null
        }
    }

    fun saveSubtitleToInternalStorage(context: Context, uri: Uri, fileName: String, videoName: String? = null): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val videoDir = File(context.filesDir, "videos")
            
            // Determine the subfolder. If videoName is provided, use it. 
            // Otherwise use the subtitle's name.
            val baseName = if (videoName != null) {
                if (videoName.contains(".")) videoName.substringBeforeLast(".") else videoName
            } else {
                if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
            }
            
            val targetDir = File(videoDir, baseName)
            if (!targetDir.exists()) targetDir.mkdirs()
            
            // If linked to a video, we might want to rename the SRT to match the video name exactly
            val targetFileName = if (videoName != null) {
                val videoBase = if (videoName.contains(".")) videoName.substringBeforeLast(".") else videoName
                "$videoBase.srt"
            } else {
                fileName
            }

            val targetFile = File(targetDir, targetFileName)
            val outputStream = FileOutputStream(targetFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            Log.e("FileUtils", "Error saving subtitle", e)
            null
        }
    }

    fun getVideoDuration(context: Context, file: File): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.fromFile(file))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLong() ?: 0L
            val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
            if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun exportVideoToMovies(context: Context, file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies")
        }

        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it).use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }
        }
    }

    fun findBestSrtForVideo(videoFile: File): File? {
        if (videoFile.isDirectory) return null
        val folder = videoFile.parentFile ?: return null
        val srtFiles = folder.listFiles()?.filter { it.extension.lowercase() == "srt" } ?: return null
        
        // 1. Exact match (case insensitive)
        srtFiles.find { it.nameWithoutExtension.equals(videoFile.nameWithoutExtension, ignoreCase = true) }?.let { return it }
        
        // 2. Contains match (e.g. "Friends S01.mp4" matches "Friends.srt")
        return srtFiles.find { 
            videoFile.nameWithoutExtension.contains(it.nameWithoutExtension, ignoreCase = true) ||
            it.nameWithoutExtension.contains(videoFile.nameWithoutExtension, ignoreCase = true)
        }
    }
}
