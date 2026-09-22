package com.example.lingoFlix.data

import androidx.room.*
import com.example.lingoFlix.model.VideoMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoMetadataDao {
    @Query("SELECT * FROM video_metadata")
    fun getAllMetadata(): Flow<List<VideoMetadata>>

    @Query("SELECT * FROM video_metadata WHERE filePath = :path LIMIT 1")
    suspend fun getMetadataForVideo(path: String): VideoMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: VideoMetadata)

    @Update
    suspend fun updateMetadata(metadata: VideoMetadata)

    @Delete
    suspend fun deleteMetadata(metadata: VideoMetadata)
    
    @Query("DELETE FROM video_metadata WHERE filePath = :path")
    suspend fun deleteByPath(path: String)
}
