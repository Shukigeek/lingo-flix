package com.example.lingoFlix.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.model.RecommendedMedia
import com.example.lingoFlix.model.DynamicSkill
import com.example.lingoFlix.model.AppConfig
import com.example.lingoFlix.model.FavoriteClip

@Database(
    entities = [VideoMetadata::class, RecommendedMedia::class, DynamicSkill::class, AppConfig::class, FavoriteClip::class], 
    version = 5, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoMetadataDao(): VideoMetadataDao
    abstract fun recommendedMediaDao(): RecommendedMediaDao
    abstract fun skillDao(): SkillDao
    abstract fun configDao(): ConfigDao
    abstract fun favoriteClipDao(): FavoriteClipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingoflix_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
