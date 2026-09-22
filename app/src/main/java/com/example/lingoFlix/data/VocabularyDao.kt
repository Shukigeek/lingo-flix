package com.example.lingoFlix.data

import androidx.room.*
import com.example.lingoFlix.model.VocabularyWord
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY nextReviewMs ASC")
    fun getAllVocabulary(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary WHERE nextReviewMs <= :currentTimeMs ORDER BY nextReviewMs ASC")
    suspend fun getWordsForReview(currentTimeMs: Long): List<VocabularyWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: VocabularyWord)

    @Update
    suspend fun updateWord(word: VocabularyWord)

    @Delete
    suspend fun deleteWord(word: VocabularyWord)

    @Query("SELECT * FROM vocabulary WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): VocabularyWord?
}
