package com.example.lingoFlix.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.lingoFlix.utils.FileUtils
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.delay
import java.io.File

@OptIn(UnstableApi::class)
object VideoPlayerController {
    
    fun createPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            playWhenReady = true
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    LingoLog.e("VideoPlayerController", "ExoPlayer Error: ${error.message}", error)
                }
            })
        }
    }

    suspend fun preparePlayer(
        exoPlayer: ExoPlayer,
        context: Context,
        targetUri: Uri,
        isClipMode: Boolean,
        detectedLanguage: String,
        preferredAudioLang: String,
        startPos: Long
    ) {
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            
            val mediaItemBuilder = MediaItem.Builder().setUri(targetUri)
            
            if (!isClipMode && targetUri.scheme == "file") {
                val videoFile = File(targetUri.path!!)
                val srtFile = FileUtils.findBestSrtForVideo(videoFile)
                if (srtFile != null && srtFile.exists()) {
                    val utf8Srt = FileUtils.getUtf8SrtFile(context, srtFile)
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(utf8Srt))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage(if (detectedLanguage == "עברית") "he" else "en")
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
                }
            }

            exoPlayer.setMediaItem(mediaItemBuilder.build(), startPos)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
            
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage(preferredAudioLang)
                .build()

            var waited = 0
            while (exoPlayer.playbackState != androidx.media3.common.Player.STATE_READY && waited < 5000) {
                delay(50)
                waited += 50
            }
            exoPlayer.play()
        } catch (e: Exception) {
            LingoLog.e("VideoPlayerController", "Error preparing player", e)
        }
    }
}
