package com.example.lingoFlix.data.repository

import com.example.lingoFlix.data.remote.TmdbApiService
import com.example.lingoFlix.model.SearchResult
import com.example.lingoFlix.utils.LingoLog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Repository responsible for fetching movie and TV show data from TMDB.
 * Adheres to SRP by focusing only on data retrieval and mapping.
 */
class TmdbRepository(private val apiService: TmdbApiService) {

    private val className: String = "TmdbRepository"

    /**
     * Searches for media across multiple categories (movies/TV).
     * Fetches additional video metadata for each result.
     */
    suspend fun search(query: String, apiKey: String): List<SearchResult> = coroutineScope {
        LingoLog.d(className, "Starting search for query: $query")
        try {
            val response = apiService.searchMulti(apiKey, query)
            val results = response.results
                .filter { it.mediaType != "person" }
                .map { result ->
                    async {
                        val videoId = fetchYoutubeVideoIdFromTmdb(result.id, result.mediaType ?: "movie", apiKey)
                        mapTmdbResultToSearchResult(result, videoId)
                    }
                }.awaitAll()
            LingoLog.d(className, "Search completed successfully with ${results.size} results")
            results
        } catch (e: Exception) {
            LingoLog.e(className, "Error during multi-search for query: $query", e)
            emptyList<SearchResult>()
        }
    }

    /**
     * Fetches daily trending media and their associated trailers.
     */
    suspend fun getTrending(apiKey: String): List<SearchResult> = coroutineScope {
        LingoLog.d(className, "Fetching trending media")
        try {
            val response = apiService.getTrending(apiKey)
            val results = response.results
                .filter { it.mediaType != "person" }
                .take(10)
                .map { result ->
                    async {
                        val videoId = fetchYoutubeVideoIdFromTmdb(result.id, result.mediaType ?: "movie", apiKey)
                        mapTmdbResultToSearchResult(result, videoId)
                    }
                }.awaitAll()
            LingoLog.d(className, "Trending fetch completed successfully")
            results
        } catch (e: Exception) {
            LingoLog.e(className, "Error fetching trending media", e)
            emptyList<SearchResult>()
        }
    }

    /**
     * Internal helper to fetch a YouTube trailer key for a specific media ID.
     */
    private suspend fun fetchYoutubeVideoIdFromTmdb(mediaId: Int, mediaType: String, apiKey: String): String? {
        return try {
            val videoResponse = if (mediaType == "tv") {
                apiService.getTvVideos(mediaId, apiKey)
            } else {
                apiService.getMovieVideos(mediaId, apiKey)
            }
            val videoId = videoResponse.results
                .firstOrNull { it.site == "YouTube" && (it.type == "Trailer" || it.type == "Teaser") }
                ?.key
            LingoLog.d(className, "Fetched video ID for $mediaType $mediaId: $videoId")
            videoId
        } catch (e: Exception) {
            LingoLog.e(className, "Failed to fetch video ID for $mediaType $mediaId", e)
            null
        }
    }

    /**
     * Maps raw TMDB API response objects to the internal SearchResult model.
     */
    private fun mapTmdbResultToSearchResult(
        result: com.example.lingoFlix.data.remote.TmdbResult, 
        youtubeVideoId: String?
    ): SearchResult {
        return SearchResult(
            id = result.id,
            title = result.name ?: result.title ?: "Unknown Title",
            imageUrl = result.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            description = result.overview ?: "No description available",
            mediaType = result.mediaType ?: "movie",
            rating = result.voteAverage ?: 0.0,
            youtubeVideoId = youtubeVideoId
        )
    }
}
