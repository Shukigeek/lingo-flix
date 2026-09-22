package com.example.lingoFlix.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/**
 * Persists the backend URL, the JWT and the last known user profile.
 * Everything is optional: the app keeps working offline when nothing is set.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lingo_session", Context.MODE_PRIVATE)
    // Shared with ServerConnectionManager so the "connect to server" card and the API client agree.
    private val serverPrefs: SharedPreferences = context.getSharedPreferences("server_connection", Context.MODE_PRIVATE)
    private val gson = Gson()

    var baseUrl: String
        get() = serverPrefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
        set(value) {
            var v = value.trim()
            if (v.isNotEmpty() && !v.startsWith("http")) v = "http://$v"
            if (v.isNotEmpty() && !v.endsWith("/")) v = "$v/"
            serverPrefs.edit().putString(KEY_BASE_URL, v).apply()
        }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var user: UserDto?
        get() = prefs.getString(KEY_USER, null)?.let { runCatching { gson.fromJson(it, UserDto::class.java) }.getOrNull() }
        set(value) = prefs.edit().putString(KEY_USER, value?.let { gson.toJson(it) }).apply()

    var onlineEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONLINE, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLINE, value).apply()

    var useLlm: Boolean
        get() = prefs.getBoolean(KEY_USE_LLM, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_LLM, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS, value).apply()

    var lastUpdateCheckMs: Long
        get() = prefs.getLong(KEY_UPDATE_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_CHECK, value).apply()

    val isLoggedIn: Boolean get() = !token.isNullOrBlank()

    fun logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    companion object {
        // 10.0.2.2 is the host machine from the Android emulator. Change in Settings for a real device.
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
        private const val KEY_ONLINE = "online_enabled"
        private const val KEY_USE_LLM = "use_llm"
        private const val KEY_TTS = "tts_enabled"
        private const val KEY_UPDATE_CHECK = "last_update_check"
    }
}
