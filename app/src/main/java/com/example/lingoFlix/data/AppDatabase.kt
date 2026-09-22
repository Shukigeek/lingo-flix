package com.example.lingoFlix.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lingoFlix.model.VideoMetadata

@Database(entities = [VideoMetadata::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoMetadataDao(): VideoMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingoflix_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
