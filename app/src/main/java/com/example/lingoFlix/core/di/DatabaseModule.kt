package com.example.lingoFlix.core.di

import android.content.Context
import androidx.room.Room
import com.example.lingoFlix.data.AppDatabase
import com.example.lingoFlix.data.RecommendedMediaDao
import com.example.lingoFlix.data.VocabularyDao
import com.example.lingoFlix.data.local.db.dao.FavoriteSentenceDao
import com.example.lingoFlix.data.local.db.dao.LearningProgressDao
import com.example.lingoFlix.data.local.db.dao.LibraryFolderDao
import com.example.lingoFlix.data.local.db.dao.MediaItemDao
import com.example.lingoFlix.data.local.db.dao.MediaMetadataDao
import com.example.lingoFlix.data.local.db.dao.SentenceDao
import com.example.lingoFlix.data.local.db.dao.SubtitleTrackDao
import com.example.lingoFlix.data.local.db.dao.WatchProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // The v2 schema predates the sentence-centric model and has no real
            // users yet, so we take the one-time reset instead of writing a
            // migration. Explicit migrations start at version 3.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideLibraryFolderDao(db: AppDatabase): LibraryFolderDao = db.libraryFolderDao()
    @Provides fun provideMediaItemDao(db: AppDatabase): MediaItemDao = db.mediaItemDao()
    @Provides fun provideMediaMetadataDao(db: AppDatabase): MediaMetadataDao = db.mediaMetadataDao()
    @Provides fun provideSubtitleTrackDao(db: AppDatabase): SubtitleTrackDao = db.subtitleTrackDao()
    @Provides fun provideSentenceDao(db: AppDatabase): SentenceDao = db.sentenceDao()
    @Provides fun provideFavoriteSentenceDao(db: AppDatabase): FavoriteSentenceDao = db.favoriteSentenceDao()
    @Provides fun provideWatchProgressDao(db: AppDatabase): WatchProgressDao = db.watchProgressDao()
    @Provides fun provideLearningProgressDao(db: AppDatabase): LearningProgressDao = db.learningProgressDao()
    @Provides fun provideRecommendedMediaDao(db: AppDatabase): RecommendedMediaDao = db.recommendedMediaDao()
    @Provides fun provideVocabularyDao(db: AppDatabase): VocabularyDao = db.vocabularyDao()
}
