package com.example.lingoFlix.data.repository

import android.util.Log
import com.example.lingoFlix.data.remote.TmdbApiService
import com.example.lingoFlix.data.remote.TmdbResponse
import com.example.lingoFlix.data.remote.TmdbResult
import com.example.lingoFlix.data.remote.VideoResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class TmdbRepositoryTest {

    @Mock
    private lateinit var mockApiService: TmdbApiService
    
    private lateinit var repository: TmdbRepository
    private val testApiKey = "test_api_key"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TmdbRepository(mockApiService)
    }

    @Test
    fun `search returns mapped search results successfully`() = runBlocking {
        // Arrange
        val query = "Friends"
        val tmdbResult = TmdbResult(
            id = 1,
            name = "Friends",
            title = null,
            overview = "Six friends in NY",
            posterPath = "/path.jpg",
            mediaType = "tv",
            voteAverage = 8.5
        )
        val response = TmdbResponse(results = listOf(tmdbResult))
        
        `when`(mockApiService.searchMulti(eq(testApiKey), eq(query), any())).thenReturn(response)
        `when`(mockApiService.getTvVideos(eq(1), eq(testApiKey))).thenReturn(VideoResponse(results = emptyList()))

        // Act
        val results = repository.search(query, testApiKey)

        // Assert
        assertEquals(1, results.size)
        assertEquals("Friends", results[0].title)
        assertEquals("tv", results[0].mediaType)
        assertEquals(8.5, results[0].rating, 0.1)
    }

    @Test
    fun `search handles api error gracefully and returns empty list`() = runBlocking {
        // Arrange
        val query = "Error"
        `when`(mockApiService.searchMulti(eq(testApiKey), eq(query), any())).thenThrow(RuntimeException("API Error"))

        // Act
        val results = repository.search(query, testApiKey)

        // Assert
        assertTrue(results.isEmpty())
    }
}
