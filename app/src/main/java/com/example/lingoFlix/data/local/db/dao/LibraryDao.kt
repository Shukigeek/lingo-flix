package com.example.lingoFlix.data.local.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.lingoFlix.data.local.db.entity.LibraryFolderEntity
import com.example.lingoFlix.data.local.db.entity.MediaItemEntity
import com.example.lingoFlix.data.local.db.entity.MediaMetadataEntity
import com.example.lingoFlix.data.local.db.entity.SubtitleTrackEntity
import com.example.lingoFlix.data.local.db.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * A media item together with everything the library and detail screens need,
 * fetched in a single observed query to avoid N+1 lookups.
 */
data class MediaItemWithDetails(
    @Embedded val item: MediaItemEntity,
    @Relation(parentColumn = "id", entityColumn = "mediaItemId")
    val metadata: MediaMetadataEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "mediaItemId")
    val subtitleTracks: List<SubtitleTrackEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "mediaItemId")
    val progress: List<WatchProgressEntity> = emptyList(),
)

@Dao
interface LibraryFolderDao {

    @Query("SELECT * FROM library_folder WHERE parentId IS :parentId ORDER BY sortOrder, name")
    fun observeChildren(parentId: Long?): Flow<List<LibraryFolderEntity>>

    @Query("SELECT * FROM library_folder ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<LibraryFolderEntity>>

    @Query("SELECT * FROM library_folder WHERE id = :id")
    suspend fun findById(id: Long): LibraryFolderEntity?

    @Query("SELECT * FROM library_folder WHERE parentId IS :parentId AND name = :name LIMIT 1")
    suspend fun findByName(parentId: Long?, name: String): LibraryFolderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folder: LibraryFolderEntity): Long

    @Update
    suspend fun update(folder: LibraryFolderEntity)

    @Query("UPDATE library_folder SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM library_folder WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Creates the folder only if one with the same name does not already exist. */
    @Transaction
    suspend fun findOrCreate(parentId: Long?, name: String, kind: String): Long {
        findByName(parentId, name)?.let { return it.id }
        return insert(LibraryFolderEntity(parentId = parentId, name = name, kind = kind))
    }
}

@Dao
interface MediaItemDao {

    @Transaction
    @Query(
        """
        SELECT * FROM media_item
        WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId
        ORDER BY seriesName, season, episode, title
        """
    )
    fun observeByFolder(folderId: Long?): Flow<List<MediaItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM media_item ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<MediaItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM media_item WHERE id = :id")
    fun observeById(id: Long): Flow<MediaItemWithDetails?>

    @Transaction
    @Query("SELECT * FROM media_item WHERE id = :id")
    suspend fun findById(id: Long): MediaItemWithDetails?

    @Query("SELECT * FROM media_item WHERE fileUri = :fileUri LIMIT 1")
    suspend fun findByUri(fileUri: String): MediaItemEntity?

    @Query("SELECT fileUri FROM media_item")
    suspend fun allUris(): List<String>

    /**
     * Most recently watched items that are not finished yet — powers the
     * "continue watching" card on the home screen.
     */
    @Transaction
    @Query(
        """
        SELECT mi.* FROM media_item mi
        INNER JOIN watch_progress wp ON wp.mediaItemId = mi.id
        WHERE wp.userId = :userId AND wp.completed = 0 AND wp.positionMs > 0
        ORDER BY wp.updatedAt DESC
        LIMIT :limit
        """
    )
    fun observeContinueWatching(userId: String, limit: Int = 10): Flow<List<MediaItemWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM media_item
        WHERE title LIKE '%' || :query || '%' OR seriesName LIKE '%' || :query || '%'
        ORDER BY title
        LIMIT :limit
        """
    )
    fun search(query: String, limit: Int = 50): Flow<List<MediaItemWithDetails>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaItemEntity): Long

    @Update
    suspend fun update(item: MediaItemEntity)

    @Query("UPDATE media_item SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<Long>, folderId: Long?)

    @Query("UPDATE media_item SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM media_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_item WHERE fileUri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("SELECT COUNT(*) FROM media_item")
    fun observeCount(): Flow<Int>
}

@Dao
interface MediaMetadataDao {

    @Query("SELECT * FROM media_metadata WHERE mediaItemId = :mediaItemId")
    suspend fun findById(mediaItemId: Long): MediaMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: MediaMetadataEntity)

    @Query("DELETE FROM media_metadata WHERE mediaItemId = :mediaItemId")
    suspend fun deleteById(mediaItemId: Long)
}

@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress WHERE userId = :userId AND mediaItemId = :mediaItemId")
    suspend fun find(userId: String, mediaItemId: Long): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE userId = :userId AND mediaItemId = :mediaItemId")
    fun observe(userId: String, mediaItemId: Long): Flow<WatchProgressEntity?>

    @Upsert
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("SELECT COUNT(*) FROM watch_progress WHERE userId = :userId AND completed = 1")
    fun observeCompletedCount(userId: String): Flow<Int>
}
