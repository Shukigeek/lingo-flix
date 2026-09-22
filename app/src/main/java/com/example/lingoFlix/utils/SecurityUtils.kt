package com.example.lingoFlix.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Utility for managing API keys and sensitive user data.
 */
object SecurityUtils {
    private const val PREFS_NAME = "secure_user_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key_"
    private const val KEY_TMDB_API = "tmdb_api_key_"
    private const val KEY_ANTHROPIC_API = "anthropic_api_key_"
    private const val KEY_OPENAI_API = "openai_api_key_"

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            LingoLog.w("SecurityUtils", "Encryption failed, falling back to standard prefs: ${e.message}")
            context.getSharedPreferences("user_api_keys_fallback", Context.MODE_PRIVATE)
        }
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

    fun saveOpenAiApiKey(context: Context, userId: String, apiKey: String) {
        getEncryptedPrefs(context).edit().putString(KEY_OPENAI_API + userId, apiKey).apply()
    }

    fun getOpenAiApiKey(context: Context, userId: String): String? {
        return getEncryptedPrefs(context).getString(KEY_OPENAI_API + userId, null)
    }
}
