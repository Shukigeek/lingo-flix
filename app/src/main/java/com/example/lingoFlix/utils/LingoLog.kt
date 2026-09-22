package com.example.lingoFlix.utils

import android.util.Log

/**
 * Robust logging utility for LingoFlix.
 */
object LingoLog {
    private const val TAG = "LingoFlix"

    fun d(className: String, message: String) {
        Log.d(TAG, "[$className] $message")
    }

    fun i(className: String, message: String) {
        Log.i(TAG, "[$className] $message")
    }

    fun w(className: String, message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[$className] $message", throwable)
    }

    fun e(className: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$className] $message", throwable)
    }
}
