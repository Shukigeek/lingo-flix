package com.example.lingoFlix.data

import androidx.room.*
import com.example.lingoFlix.model.RecommendedMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendedMediaDao {
    @Query("SELECT * FROM recommended_media ORDER BY isDeveloperPick DESC, title ASC")
    fun getAllRecommendations(): Flow<List<RecommendedMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(media: RecommendedMedia)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mediaList: List<RecommendedMedia>)

    @Delete
    suspend fun deleteRecommendation(media: RecommendedMedia)

    @Query("SELECT * FROM recommended_media WHERE tmdbId = :id")
    suspend fun getById(id: Int): RecommendedMedia?
}
