package com.example.lingoFlix.utils

import android.util.Log

/**
 * Standardized logging utility for LingoFlix.
 * Ensures consistent tag usage and levels.
 */
object LingoLog {
    private const val TAG = "LingoFlix"

    fun d(className: String, message: String) {
        Log.d(TAG, "[$className] $message")
    }

    fun i(className: String, message: String) {
        Log.i(TAG, "[$className] $message")
    }

    fun w(className: String, message: String) {
        Log.w(TAG, "[$className] $message")
    }

    fun e(className: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$className] $message", throwable)
        } else {
            Log.e(TAG, "[$className] $message")
        }
    }
}
