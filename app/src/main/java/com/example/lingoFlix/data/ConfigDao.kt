package com.example.lingoFlix.data

import androidx.room.*
import com.example.lingoFlix.model.AppConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 'global_config' LIMIT 1")
    fun getConfig(): Flow<AppConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AppConfig)
}
