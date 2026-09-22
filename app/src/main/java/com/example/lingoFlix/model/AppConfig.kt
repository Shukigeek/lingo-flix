package com.example.lingoFlix.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Global application settings managed by the developer from the phone.
 */
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: String = "global_config",
    val targetLanguage: String = "עברית",
    val sourceLanguage: String = "אנגלית",
    val isAutoDiscoveryEnabled: Boolean = true,
    val telegramBotToken: String? = null,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
