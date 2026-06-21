package com.example.lingoFlix.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class TmdbResponse(
    @SerializedName("results") val results: List<TmdbResult>
)

data class TmdbResult(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

data class VideoResponse(
    @SerializedName("results") val results: List<VideoResult>
)

data class VideoResult(
    @SerializedName("key") val key: String,
    @SerializedName("site") val site: String,
    @SerializedName("type") val type: String
)

interface TmdbApiService {
    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "he-IL"
    ): TmdbResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "he-IL"
    ): TmdbResponse

    @GET("trending/all/day")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "he-IL"
    ): TmdbResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @retrofit2.http.Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): VideoResponse

    @GET("tv/{tv_id}/videos")
    suspend fun getTvVideos(
        @retrofit2.http.Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): VideoResponse

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"

        fun create(): TmdbApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TmdbApiService::class.java)
        }
    }
}
