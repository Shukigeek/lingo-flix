package com.example.lingoFlix.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.lingoFlix.R
import com.example.lingoFlix.utils.LingoLog

object SoundManager {
    private var soundPool: SoundPool? = null
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()

            // These IDs will be 0 if the resources are missing, 
            // and soundPool?.play will just do nothing.
            // We expect correct_sound and wrong_sound in res/raw
            correctSoundId = soundPool?.load(context, R.raw.correct_sound, 1) ?: 0
            wrongSoundId = soundPool?.load(context, R.raw.wrong_sound, 1) ?: 0
            
            isInitialized = true
            LingoLog.d("SoundManager", "Initialized successfully")
        } catch (e: Exception) {
            LingoLog.e("SoundManager", "Error initializing SoundManager", e)
        }
    }

    fun playCorrect() {
        if (correctSoundId != 0) {
            soundPool?.play(correctSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    fun playWrong() {
        if (wrongSoundId != 0) {
            soundPool?.play(wrongSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
