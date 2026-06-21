package com.example.lingoFlix.utils

import android.util.Log

/**
 * Centralized logging utility for LingoFlix.
 * Ensures consistent logging format across the application.
 */
object LingoLog {
    private const val TAG_PREFIX = "LingoFlix_"

    fun d(className: String, message: String) {
        Log.d("${TAG_PREFIX}$className", message)
    }

    fun e(className: String, message: String, throwable: Throwable? = null) {
        Log.e("${TAG_PREFIX}$className", message, throwable)
    }

    fun i(className: String, message: String) {
        Log.i("${TAG_PREFIX}$className", message)
    }

    fun w(className: String, message: String) {
        Log.w("${TAG_PREFIX}$className", message)
    }
}
