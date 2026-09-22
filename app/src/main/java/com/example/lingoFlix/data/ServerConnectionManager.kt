package com.example.lingoFlix.data

import android.content.Context
import android.util.Log

private const val TAG = "ServerConnectionManager"
private const val PREFS_NAME = "server_connection"
private const val KEY_BASE_URL = "base_url"

/**
 * Persists the address of the home server (Raspberry Pi, or a computer for now during testing)
 * that hosts the LingoFlix backend, so the app knows where to send API requests.
 */
class ServerConnectionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedServerBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun saveServerBaseUrl(rawInput: String): String {
        val normalized = normalizeAddressIntoBaseUrl(rawInput)
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        Log.i(TAG, "Saved server base URL: $normalized")
        return normalized
    }

    fun clearSavedServer() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }

    /** Turns loose user input like "192.168.1.50:8000" into a valid Retrofit base URL ending in "/". */
    private fun normalizeAddressIntoBaseUrl(rawInput: String): String {
        var address = rawInput.trim()
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://$address"
        }
        if (!address.endsWith("/")) {
            address = "$address/"
        }
        return address
    }
}
