package com.example.lingoFlix.data.repository

import com.example.lingoFlix.data.RecommendedMediaDao
import com.example.lingoFlix.data.remote.TmdbApiService
import com.example.lingoFlix.model.RecommendedMedia
import com.example.lingoFlix.model.SearchResult
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository that manages curated recommendations from the developer.
 * Combines local database storage with online metadata from TMDB.
 */
class RecommendedMediaRepository(
    private val dao: RecommendedMediaDao,
    private val apiService: TmdbApiService
) {
    private val className = "RecommendedMediaRepository"

    /**
     * Returns a stream of recommended media, mapped to SearchResult for UI compatibility.
     */
    fun getRecommendedMediaFlow(): Flow<List<SearchResult>> {
        return dao.getAllRecommendations().map { list ->
            list.map { it.toSearchResult() }
        }
    }

    /**
     * Adds a new recommendation to the database.
     */
    suspend fun addRecommendation(item: SearchResult) {
        LingoLog.d(className, "Adding manual recommendation: ${item.title} (${item.id})")
        try {
            val recommended = RecommendedMedia(
                tmdbId = item.id,
                title = item.title,
                mediaType = item.mediaType,
                cachedDescription = item.description,
                cachedRating = item.rating,
                cachedImageUrl = item.imageUrl,
                customYoutubeTrailerId = item.youtubeVideoId,
                isDeveloperPick = true
            )
            dao.insertRecommendation(recommended)
        } catch (e: Exception) {
            LingoLog.e(className, "Failed to add recommendation", e)
        }
    }

    /**
     * Seeds the database with initial developer picks if empty.
     */
    suspend fun seedDeveloperPicks() {
        LingoLog.i(className, "Seeding developer picks")
        val initialPicks = listOf(
            RecommendedMedia(
                tmdbId = 1668,
                title = "Friends",
                mediaType = "tv",
                customTelegramLink = "https://t.me/Friends_Hebrew_Sub",
                customYoutubeTrailerId = "hDNNmeeJs1Q",
                cachedDescription = "שישה חברים צעירים שחיים במנהטן ומתמודדים עם החיים, אהבה ועבודה.",
                cachedRating = 8.5,
                cachedImageUrl = "https://image.tmdb.org/t/p/w500/fob2vYm5998Ar936IIn9C9YpXcr.jpg"
            ),
            RecommendedMedia(
                tmdbId = 1396,
                title = "Breaking Bad",
                mediaType = "tv",
                customTelegramLink = "https://t.me/Breaking_Bad_Hebrew",
                customYoutubeTrailerId = "HhesaQXLuRY",
                cachedDescription = "מורה לכימיה בתיכון שמאובחן בסרטן ריאות סופני פונה לייצור ומכירת מתאמפטמין כדי להבטיח את עתידה הכלכלי של משפחתו.",
                cachedRating = 8.9,
                cachedImageUrl = "https://image.tmdb.org/t/p/w500/ggm8bbub63OwoE1Z977jZ0hzR3H.jpg"
            ),
            RecommendedMedia(
                tmdbId = 66732,
                title = "Stranger Things",
                mediaType = "tv",
                customYoutubeTrailerId = "b9EkMc79ZSU",
                cachedDescription = "כשילד קטן נעלם, עיירה קטנה חושפת תעלומה הכוללת ניסויים סודיים, כוחות על-טבעיים מפחידים וילדה אחת מוזרה.",
                cachedRating = 8.6,
                cachedImageUrl = "https://image.tmdb.org/t/p/w500/x2LSRt21zpbvFmwiFTbe26Y9f0X.jpg"
            ),
            RecommendedMedia(
                tmdbId = 63174,
                title = "Lucifer",
                mediaType = "tv",
                customTelegramLink = "https://t.me/Lucifer_Hebrew",
                customYoutubeTrailerId = "X4bF_KnwY1U",
                cachedDescription = "לוציפר מורנינגסטאר, מלך הגיהנום המשועמם, עובר ללוס אנג'לס ופותח מועדון לילה.",
                cachedRating = 8.5,
                cachedImageUrl = "https://image.tmdb.org/t/p/w500/ekZobS8isE6SqcNBbq3PqdfC2Y.jpg"
            ),
            RecommendedMedia(
                tmdbId = 27205,
                title = "Inception",
                mediaType = "movie",
                customYoutubeTrailerId = "YoHD9XEInc0",
                cachedDescription = "גנב שגונב סודות תאגידיים דרך שימוש בטכנולוגיית שיתוף חלומות מקבל את המשימה ההפוכה: להשתיל רעיון במוחו של מנכ\"ל.",
                cachedRating = 8.4,
                cachedImageUrl = "https://image.tmdb.org/t/p/w500/edv5CZvRjY9y9o9szCcI0BvRzdf.jpg"
            ),
            RecommendedMedia(
                tmdbId = 157336,
                title = "Interstellar",
                mediaType = "movie",
                customYoutubeTrailerId = "zSWdZVtXT7E",
                cachedDescription = "צוות חוקרים נוסע מעבר לגלקסיה הזו כדי לגלות אם לאנושות יש עתיד בין הכוכבים.",
                cachedRating = 8.4,
                cachedImageUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlSabaC.jpg"
            )
        )
        dao.insertAll(initialPicks)
    }

    private fun RecommendedMedia.toSearchResult(): SearchResult {
        return SearchResult(
            id = tmdbId,
            title = title,
            imageUrl = cachedImageUrl,
            description = cachedDescription ?: "",
            mediaType = mediaType,
            rating = cachedRating,
            youtubeVideoId = customYoutubeTrailerId,
            customTelegramLink = customTelegramLink
        )
    }
}
