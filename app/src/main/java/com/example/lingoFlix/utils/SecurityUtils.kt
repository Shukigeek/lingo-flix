package com.example.lingoFlix.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val PREFS_NAME = "secure_user_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key_"

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveUserApiKey(context: Context, userId: String, apiKey: String) {
        getEncryptedPrefs(context).edit().putString(KEY_GEMINI_API + userId, apiKey).apply()
    }

    fun getUserApiKey(context: Context, userId: String): String? {
        return getEncryptedPrefs(context).getString(KEY_GEMINI_API + userId, null)
    }
}
