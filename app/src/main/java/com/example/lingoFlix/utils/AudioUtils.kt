package com.example.lingoFlix.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object AudioUtils {
    private const val TAG = "AudioUtils"

    /**
     * Extracts the first audio track from a video file and saves it as an M4A file.
     * This is useful for reducing upload size for transcription services like Whisper.
     */
    fun extractAudioFromVideo(videoFile: File, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(videoFile.absolutePath)
            
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }
            
            if (audioTrackIndex == -1 || format == null) {
                LingoLog.e(TAG, "No audio track found in ${videoFile.name}")
                return false
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val writeTrackIndex = muxer.addTrack(format)
            muxer.start()
            
            val bufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).let { if (it <= 0) 1024 * 1024 else it }
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                
                if (bufferInfo.size < 0) {
                    break
                }
                
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }
            
            return true
        } catch (e: Exception) {
            LingoLog.e(TAG, "Error extracting audio", e)
            return false
        } finally {
            try {
                extractor?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) { /* ignore */ }
        }
    }
}
