package com.example.lingoFlix.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-created folder in the library tree.
 *
 * Folders are purely logical: they do not have to mirror the on-disk layout,
 * which lets the user organise content however they like (see project book §2).
 */
@Entity(
    tableName = "library_folder",
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("parentId")],
)
data class LibraryFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** `null` means this is a root folder. */
    val parentId: Long? = null,
    val name: String,
    /** One of [FolderKind]. */
    val kind: String = FolderKind.CUSTOM,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

object FolderKind {
    const val MOVIES = "MOVIES"
    const val SERIES = "SERIES"
    const val SEASON = "SEASON"
    const val CUSTOM = "CUSTOM"
}

/**
 * A single playable item: a film or one episode of a series.
 *
 * [fileUri] is the stable identity of the item. For local content this is a SAF
 * `content://` URI for which we hold a persisted read permission.
 */
@Entity(
    tableName = "media_item",
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [
        Index(value = ["fileUri"], unique = true),
        Index("folderId"),
        Index("seriesName"),
        Index(value = ["seriesName", "season", "episode"]),
    ],
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val title: String,
    val fileUri: String,
    /** One of [MediaSourceType]. */
    val sourceType: String = MediaSourceType.LOCAL,
    /** One of [MediaKind]. */
    val kind: String = MediaKind.MOVIE,
    val seriesName: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val durationMs: Long = 0,
    val thumbnailPath: String? = null,
    val sizeBytes: Long = 0,
    val addedAt: Long = System.currentTimeMillis(),
)

object MediaSourceType {
    const val LOCAL = "LOCAL"
    const val PC_CLIENT = "PC_CLIENT"
    const val URL = "URL"
}

object MediaKind {
    const val MOVIE = "MOVIE"
    const val EPISODE = "EPISODE"
}

/**
 * Metadata fetched from an external catalogue (TMDb/OMDb), cached locally so the
 * library renders instantly and works offline (project book §7.6).
 */
@Entity(
    tableName = "media_metadata",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class MediaMetadataEntity(
    @PrimaryKey val mediaItemId: Long,
    val tmdbId: Int? = null,
    val imdbId: String? = null,
    val year: Int? = null,
    val imdbRating: Double? = null,
    val voteCount: Int? = null,
    /** Comma separated, e.g. `"Action,Sci-Fi"`. */
    val genres: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    /** JSON array of actor names. */
    @ColumnInfo(name = "castJson") val castJson: String? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
)
