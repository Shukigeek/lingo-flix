package com.example.lingoFlix.data.repository

import com.example.lingoFlix.data.remote.TmdbApiService
import com.example.lingoFlix.model.SearchResult

class TmdbRepository(private val apiService: TmdbApiService) {

    suspend fun search(query: String, apiKey: String): List<SearchResult> {
        return try {
            // User specifically asked for search/tv flow
            val response = apiService.searchTv(apiKey, query)
            response.results.map { result ->
                SearchResult(
                    id = result.id,
                    title = result.name ?: result.title ?: "Unknown",
                    imageUrl = result.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    description = result.overview ?: "",
                    mediaType = "tv"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrending(apiKey: String): List<SearchResult> {
        return try {
            val response = apiService.getTrending(apiKey)
            response.results.filter { it.mediaType != "person" }.take(20).map { result ->
                SearchResult(
                    id = result.id,
                    title = result.title ?: result.name ?: "Unknown",
                    imageUrl = result.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    description = result.overview ?: "",
                    mediaType = result.mediaType ?: "movie"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
