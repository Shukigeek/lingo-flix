package com.example.lingoFlix.data.repository

import android.content.Context
import com.example.lingoFlix.core.di.IoDispatcher
import com.example.lingoFlix.data.UserStatsManager
import com.example.lingoFlix.data.local.db.dao.FavoriteSentenceDao
import com.example.lingoFlix.data.local.db.dao.LearningProgressDao
import com.example.lingoFlix.data.local.db.dao.MediaItemDao
import com.example.lingoFlix.data.local.db.dao.WatchProgressDao
import com.example.lingoFlix.data.local.db.entity.LOCAL_USER_ID
import com.example.lingoFlix.data.local.db.entity.LearningProgressEntity
import com.example.lingoFlix.data.mapper.toDomain
import com.example.lingoFlix.domain.model.LearningStats
import com.example.lingoFlix.domain.model.SavedSentence
import com.example.lingoFlix.domain.repository.PracticeRepository
import com.example.lingoFlix.domain.usecase.Sm2Scheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The spaced-repetition side of the app: the review queue, answer recording
 * and the aggregate numbers shown on the home and profile screens.
 */
@Singleton
class PracticeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val learningProgressDao: LearningProgressDao,
    private val favoriteDao: FavoriteSentenceDao,
    private val mediaItemDao: MediaItemDao,
    private val watchProgressDao: WatchProgressDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PracticeRepository {

    /** Streak and XP still live in SharedPreferences; reused rather than duplicated. */
    private val userStats by lazy { UserStatsManager(context) }

    override suspend fun dueQueue(limit: Int): List<SavedSentence> = withContext(ioDispatcher) {
        learningProgressDao
            .dueQueue(LOCAL_USER_ID, System.currentTimeMillis(), limit)
            .map { it.toDomain() }
    }

    /**
     * `now` is bound when the flow is collected. That is deliberate: re-running
     * the query on every clock tick would thrash the database, and the count is
     * refreshed anyway as soon as the user answers a card.
     */
    override fun observeDueCount(): Flow<Int> =
        learningProgressDao.observeDueCount(LOCAL_USER_ID, System.currentTimeMillis())

    override suspend fun recordAnswer(sentenceId: Long, quality: Int) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val current = learningProgressDao.find(LOCAL_USER_ID, sentenceId)
        val result = Sm2Scheduler.schedule(current?.toDomain(), quality, now)

        learningProgressDao.upsert(
            LearningProgressEntity(
                userId = LOCAL_USER_ID,
                sentenceId = sentenceId,
                repetitions = result.repetitions,
                intervalDays = result.intervalDays,
                easeFactor = result.easeFactor,
                lastReviewAt = now,
                nextReviewAt = result.nextReviewAt,
                lapses = result.lapses,
                masteryLevel = result.masteryLevel,
            )
        )
        // Reviewing counts as activity, which is what keeps the streak alive.
        userStats.markActivityToday()
    }

    override fun observeStats(): Flow<LearningStats> = combine(
        mediaItemDao.observeCount(),
        watchProgressDao.observeCompletedCount(LOCAL_USER_ID),
        favoriteDao.observeCount(LOCAL_USER_ID),
        learningProgressDao.observeLearnedCount(LOCAL_USER_ID),
        learningProgressDao.observeDueCount(LOCAL_USER_ID, System.currentTimeMillis()),
    ) { mediaCount, completed, saved, learned, due ->
        LearningStats(
            mediaCount = mediaCount,
            completedCount = completed,
            savedSentenceCount = saved,
            learnedSentenceCount = learned,
            dueCount = due,
            streakDays = userStats.getStreak(),
            totalXp = userStats.getXP(),
        )
    }
}
