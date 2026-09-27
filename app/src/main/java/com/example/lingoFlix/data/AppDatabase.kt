package com.example.lingoFlix.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lingoFlix.data.local.db.dao.FavoriteSentenceDao
import com.example.lingoFlix.data.local.db.dao.LearningProgressDao
import com.example.lingoFlix.data.local.db.dao.LibraryFolderDao
import com.example.lingoFlix.data.local.db.dao.MediaItemDao
import com.example.lingoFlix.data.local.db.dao.MediaMetadataDao
import com.example.lingoFlix.data.local.db.dao.SentenceDao
import com.example.lingoFlix.data.local.db.dao.SubtitleTrackDao
import com.example.lingoFlix.data.local.db.dao.WatchProgressDao
import com.example.lingoFlix.data.local.db.entity.FavoriteSentenceEntity
import com.example.lingoFlix.data.local.db.entity.LearningProgressEntity
import com.example.lingoFlix.data.local.db.entity.LibraryFolderEntity
import com.example.lingoFlix.data.local.db.entity.MediaItemEntity
import com.example.lingoFlix.data.local.db.entity.MediaMetadataEntity
import com.example.lingoFlix.data.local.db.entity.SentenceEntity
import com.example.lingoFlix.data.local.db.entity.SentenceFtsEntity
import com.example.lingoFlix.data.local.db.entity.SubtitleTrackEntity
import com.example.lingoFlix.data.local.db.entity.WatchProgressEntity
import android.content.Context
import androidx.room.Room
import com.example.lingoFlix.model.RecommendedMedia
import com.example.lingoFlix.model.VideoMetadata
import com.example.lingoFlix.model.VocabularyWord

/**
 * The single local source of truth.
 *
 * Version 3 introduces the sentence-centric schema described in the project
 * book (§11). The instance is provided by Hilt — see `DatabaseModule` — so no
 * manual singleton is needed here.
 */
@Database(
    entities = [
        // Library
        LibraryFolderEntity::class,
        MediaItemEntity::class,
        MediaMetadataEntity::class,
        // Subtitles and sentences
        SubtitleTrackEntity::class,
        SentenceEntity::class,
        SentenceFtsEntity::class,
        // Learning
        FavoriteSentenceEntity::class,
        WatchProgressEntity::class,
        LearningProgressEntity::class,
        // Carried over from v2
        RecommendedMedia::class,
        VocabularyWord::class,
        // TODO(cleanup): remove once the legacy video list/player screens are gone.
        VideoMetadata::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun libraryFolderDao(): LibraryFolderDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun mediaMetadataDao(): MediaMetadataDao
    abstract fun subtitleTrackDao(): SubtitleTrackDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun favoriteSentenceDao(): FavoriteSentenceDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun learningProgressDao(): LearningProgressDao

    abstract fun recommendedMediaDao(): RecommendedMediaDao
    abstract fun vocabularyDao(): VocabularyDao

    /** Legacy accessor kept alive for the screens still being migrated. */
    abstract fun videoMetadataDao(): VideoMetadataDao

    companion object {
        const val NAME = "lingoflix_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Legacy entry point for composables that have not been migrated to Hilt yet.
         *
         * Prefer injecting [AppDatabase] (or a repository) instead. This will be
         * deleted once every caller goes through DI.
         */
        @Deprecated("Inject the database or a repository with Hilt instead.")
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME,
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
