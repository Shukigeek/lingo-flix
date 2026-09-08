package com.example.lingoFlix.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LingoApiService {
    @POST("/translate")
    suspend fun translateWord(@Body request: TranslationRequest): TranslationResponse

    @POST("/activity")
    suspend fun updateActivity(@Body update: ActivityUpdate): Map<String, Any>

    @GET("/stats/difficult-words")
    suspend fun getDifficultWords(): List<WordStat>

    @GET("/health")
    suspend fun checkServerHealth(): Map<String, String>

    @GET("/check-update")
    suspend fun checkUpdate(): Map<String, String>

    @GET("/movies/library")
    suspend fun getCloudLibrary(): List<CloudMovie>

    @GET("/movies/info/{movie_name}")
    suspend fun getMovieProInfo(@retrofit2.http.Path("movie_name") name: String): Map<String, Any>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000" // 10.0.2.2 is localhost for Android Emulator

        fun create(): LingoApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LingoApiService::class.java)
        }
    }
}
