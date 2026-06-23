package com.example.lingoFlix.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val PREFS_NAME = "secure_user_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key_"
    private const val KEY_TMDB_API = "tmdb_api_key_"
    private const val KEY_ANTHROPIC_API = "anthropic_api_key_"

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        // Since we are having issues with key persistence, let's use a standard Prefs for the API key 
        // until we stabilize the encryption issues. 
        return context.getSharedPreferences("user_api_keys", Context.MODE_PRIVATE)
    }

    fun saveUserApiKey(context: Context, userId: String, apiKey: String) {
        getEncryptedPrefs(context).edit().putString(KEY_GEMINI_API + userId, apiKey).apply()
    }

    fun getUserApiKey(context: Context, userId: String): String? {
        return getEncryptedPrefs(context).getString(KEY_GEMINI_API + userId, null)
    }

    fun saveTmdbApiKey(context: Context, userId: String, apiKey: String) {
        getEncryptedPrefs(context).edit().putString(KEY_TMDB_API + userId, apiKey).apply()
    }

    fun getTmdbApiKey(context: Context, userId: String): String? {
        return getEncryptedPrefs(context).getString(KEY_TMDB_API + userId, null)
    }

    fun saveAnthropicApiKey(context: Context, userId: String, apiKey: String) {
        getEncryptedPrefs(context).edit().putString(KEY_ANTHROPIC_API + userId, apiKey).apply()
    }

    fun getAnthropicApiKey(context: Context, userId: String): String? {
        return getEncryptedPrefs(context).getString(KEY_ANTHROPIC_API + userId, null)
    }
}
