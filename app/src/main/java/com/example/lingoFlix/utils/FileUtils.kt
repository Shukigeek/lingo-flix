package com.example.lingoFlix.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

object FileUtils {
    fun getFileName(context: Context, uri: Uri): String? {
        LingoLog.d("FileUtils", "getFileName for: $uri")
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                        LingoLog.d("FileUtils", "Found DISPLAY_NAME: $name")
                    }
                }
            }
        } catch (e: Exception) {
            LingoLog.e("FileUtils", "Error getting file name from cursor", e)
        }

        if (name == null) {
            name = uri.lastPathSegment
            LingoLog.d("FileUtils", "Fallback to lastPathSegment: $name")
        }
        
        return name
    }

    fun saveVideoToInternalStorage(context: Context, uri: Uri, fileName: String, targetDir: File? = null): File? {
        LingoLog.d("FileUtils", "saveVideoToInternalStorage: $fileName to $targetDir")
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val videoDir = targetDir ?: File(context.filesDir, "videos")
            
            if (!videoDir.exists()) videoDir.mkdirs()
            
            val targetFile = File(videoDir, fileName)
            val outputStream = FileOutputStream(targetFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Create initial metadata for the database
            val metadata = createInitialMetadata(targetFile)
            LingoLog.d("FileUtils", "Metadata created: title='${metadata.title}', filePath='${metadata.filePath}'")
            val database = com.example.lingoFlix.data.AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                database.videoMetadataDao().insertMetadata(metadata)
                LingoLog.d("FileUtils", "Metadata inserted into DB for: ${metadata.filePath}")
            }

            targetFile
        } catch (e: Exception) {
            LingoLog.e("FileUtils", "Error saving video", e)
            null
        }
    }

    private fun createInitialMetadata(file: File): com.example.lingoFlix.model.VideoMetadata {
        val name = file.name
        // Try to parse Season/Episode (e.g., S01E05)
        val regex = Regex("[sS](\\d{1,2})[eE](\\d{1,2})")
        val match = regex.find(name)
        
        return com.example.lingoFlix.model.VideoMetadata(
            filePath = file.absolutePath,
            title = name,
            season = match?.groupValues?.get(1)?.toIntOrNull(),
            episode = match?.groupValues?.get(2)?.toIntOrNull()
        )
    }

    fun saveSubtitleToInternalStorage(context: Context, uri: Uri, fileName: String, targetDir: File? = null, videoName: String? = null): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val subDir = targetDir ?: File(context.filesDir, "videos")
            
            if (!subDir.exists()) subDir.mkdirs()
            
            // If linked to a video, we might want to rename the SRT to match the video name exactly
            val targetFileName = if (videoName != null) {
                val videoBase = if (videoName.contains(".")) videoName.substringBeforeLast(".") else videoName
                "$videoBase.srt"
            } else {
                fileName
            }

            val targetFile = File(subDir, targetFileName)
            val outputStream = FileOutputStream(targetFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            LingoLog.e("FileUtils", "Error saving subtitle", e)
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

    fun getVideoThumbnail(context: Context, file: File): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.fromFile(file))
            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) // 1 second in
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
        
        // 1. Look in the same folder as the video
        val folder = videoFile.parentFile ?: return null
        val srtFiles = folder.listFiles()?.filter { it.extension.lowercase() == "srt" } ?: emptyList()
        
        // 1a. Exact match
        srtFiles.find { it.nameWithoutExtension.equals(videoFile.nameWithoutExtension, ignoreCase = true) }?.let { return it }
        
        // 1b. Contains match
        srtFiles.find { 
            videoFile.nameWithoutExtension.contains(it.nameWithoutExtension, ignoreCase = true) ||
            it.nameWithoutExtension.contains(videoFile.nameWithoutExtension, ignoreCase = true)
        }?.let { return it }

        // 2. If video is inside a package folder, look in the parent folder too
        val grandParent = folder.parentFile
        if (grandParent != null && grandParent.name == "videos") {
            val parentSrtFiles = grandParent.listFiles()?.filter { it.extension.lowercase() == "srt" } ?: emptyList()
            
            // Exact match in parent
            parentSrtFiles.find { it.nameWithoutExtension.equals(videoFile.nameWithoutExtension, ignoreCase = true) }?.let { return it }
            
            // Match with folder name (since often video and folder share name)
            parentSrtFiles.find { it.nameWithoutExtension.equals(folder.name, ignoreCase = true) }?.let { return it }
        }

        return null
    }

    /**
     * Ensures the SRT file is in UTF-8 for ExoPlayer, which can struggle with other encodings.
     * Returns a temporary file if conversion was needed.
     */
    fun getUtf8SrtFile(context: Context, srtFile: File): File {
        return try {
            val bytes = srtFile.readBytes()
            val encoding = SrtParser.detectEncoding(bytes)
            if (encoding == Charsets.UTF_8) return srtFile
            
            val content = String(bytes, encoding)
            val tempFile = File(context.cacheDir, "temp_sub_${srtFile.nameWithoutExtension}.srt")
            tempFile.writeText(content, Charsets.UTF_8)
            tempFile
        } catch (e: Exception) {
            srtFile
        }
    }
}
