package com.example.lingoFlix.data.remote

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class AppConfig(
    val featuredVideoUrl: String? = null,
    val announcement: String? = null,
    val recommendedSeries: List<String> = emptyList(),
    val appVersion: String = "1.0.0"
)

object RemoteConfigManager {
    private const val CONFIG_URL = "https://gist.githubusercontent.com/Shukigeek/lingo-flix-config/raw/config.json" // Placeholder
    private val gson = Gson()

    suspend fun fetchConfig(): AppConfig? = withContext(Dispatchers.IO) {
        try {
            val json = URL(CONFIG_URL).readText()
            gson.fromJson(json, AppConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
