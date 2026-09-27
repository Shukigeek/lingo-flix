package com.example.lingoFlix.core.di

import com.example.lingoFlix.data.repository.LibraryRepositoryImpl
import com.example.lingoFlix.data.repository.PracticeRepositoryImpl
import com.example.lingoFlix.data.repository.SentenceRepositoryImpl
import com.example.lingoFlix.data.repository.SubtitleRepositoryImpl
import com.example.lingoFlix.domain.repository.LibraryRepository
import com.example.lingoFlix.domain.repository.PracticeRepository
import com.example.lingoFlix.domain.repository.SentenceRepository
import com.example.lingoFlix.domain.repository.SubtitleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds each repository interface to its implementation.
 *
 * `@Binds` rather than `@Provides` so Dagger reuses the constructor injection
 * already declared on the implementations, and consumers only ever see the
 * domain interfaces — which is what lets ViewModels be tested with fakes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(impl: SubtitleRepositoryImpl): SubtitleRepository

    @Binds
    @Singleton
    abstract fun bindSentenceRepository(impl: SentenceRepositoryImpl): SentenceRepository

    @Binds
    @Singleton
    abstract fun bindPracticeRepository(impl: PracticeRepositoryImpl): PracticeRepository
}
