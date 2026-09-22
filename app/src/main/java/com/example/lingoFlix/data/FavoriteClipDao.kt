package com.example.lingoFlix.data

import androidx.room.*
import com.example.lingoFlix.model.FavoriteClip
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteClipDao {
    @Query("SELECT * FROM favorite_clips")
    fun getAllFavorites(): Flow<List<FavoriteClip>>

    @Query("SELECT * FROM favorite_clips")
    suspend fun getAllFavoritesList(): List<FavoriteClip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(clip: FavoriteClip)

    @Delete
    suspend fun deleteFavorite(clip: FavoriteClip)
    
    @Query("DELETE FROM favorite_clips WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_clips WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>
}
