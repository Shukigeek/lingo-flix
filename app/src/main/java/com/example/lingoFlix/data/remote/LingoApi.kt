package com.example.lingoFlix.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LingoApi {
    // meta
    @GET("health") suspend fun health(): HealthDto
    @GET("api/v1/app/version") suspend fun appVersion(): AppVersionDto
    @GET("api/v1/ai/status") suspend fun aiStatus(): AiStatusDto

    // auth
    @POST("api/v1/auth/register") suspend fun register(@Body body: RegisterRequest): TokenResponse
    @POST("api/v1/auth/login") suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("api/v1/auth/guest") suspend fun guest(): TokenResponse
    @POST("api/v1/auth/upgrade") suspend fun upgrade(@Body body: RegisterRequest): TokenResponse
    @GET("api/v1/auth/me") suspend fun me(): UserDto
    @PATCH("api/v1/auth/me") suspend fun updateMe(@Body body: UserUpdate): UserDto

    // catalog
    @GET("api/v1/series") suspend fun series(@Query("language") language: String? = null): List<SeriesDto>
    @GET("api/v1/series/{slug}/episodes") suspend fun episodes(@Path("slug") slug: String): List<EpisodeDto>
    @GET("api/v1/sentences") suspend fun sentences(
        @Query("series_slug") seriesSlug: String? = null,
        @Query("episode_id") episodeId: Int? = null,
        @Query("q") q: String? = null,
        @Query("cefr") cefr: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("random_order") randomOrder: Boolean = false
    ): List<SentenceDto>
    @POST("api/v1/sentences/import-srt") suspend fun importSrt(@Body body: SrtImportRequest): SrtImportResult
    @GET("api/v1/daily") suspend fun daily(@Query("size") size: Int = 7): DailyChallengeDto
    @POST("api/v1/daily/complete") suspend fun completeDaily(@Body body: DailyChallengeSubmit): DailyChallengeDto

    // quiz
    @POST("api/v1/quiz/generate") suspend fun generateQuiz(@Body body: QuizRequest): QuizDto
    @POST("api/v1/quiz/batch") suspend fun batchQuiz(@Body body: BatchQuizRequest): List<QuizDto>
    @POST("api/v1/quiz/check") suspend fun checkAnswer(@Body body: AnswerCheckRequest): AnswerCheckResult
    @POST("api/v1/quiz/hearts/refill") suspend fun refillHearts(): Map<String, Int>
    @GET("api/v1/quiz/review/due") suspend fun dueReviews(@Query("limit") limit: Int = 20): List<ReviewDto>
    @GET("api/v1/quiz/review/count") suspend fun reviewCount(): ReviewCountDto

    // progress
    @GET("api/v1/stats") suspend fun stats(@Query("days") days: Int = 14): StatsDto
    @GET("api/v1/achievements") suspend fun achievements(): List<AchievementDto>
    @GET("api/v1/leaderboard") suspend fun leaderboard(@Query("weekly") weekly: Boolean = false, @Query("limit") limit: Int = 50): List<LeaderboardEntryDto>
    @GET("api/v1/favorites") suspend fun favorites(): List<FavoriteDto>
    @PUT("api/v1/favorites") suspend fun addFavorite(@Body body: FavoriteDto): FavoriteDto
    @DELETE("api/v1/favorites/{clipId}") suspend fun removeFavorite(@Path("clipId", encoded = true) clipId: String): retrofit2.Response<Unit>
    @POST("api/v1/favorites/sync") suspend fun syncFavorites(@Body body: List<FavoriteDto>): List<FavoriteDto>
    @GET("api/v1/vocabulary") suspend fun vocabulary(@Query("weak_only") weakOnly: Boolean = false): List<VocabularyDto>
    @GET("api/v1/videos") suspend fun videos(): List<VideoSourceDto>
    @POST("api/v1/videos") suspend fun addVideo(@Body body: VideoSourceDto): VideoSourceDto
    @PATCH("api/v1/videos/{id}/progress") suspend fun updateVideoProgress(@Path("id") id: Int, @Body body: ProgressUpdate): VideoSourceDto
    @DELETE("api/v1/videos/{id}") suspend fun deleteVideo(@Path("id") id: Int): retrofit2.Response<Unit>

    // media
    @POST("api/v1/media/resolve") suspend fun resolveMedia(@Body body: ResolveMediaRequest): ResolvedMediaDto
    @GET("api/v1/media/youtube/{id}/subtitles.srt") suspend fun youtubeSubtitles(@Path("id") id: String, @Query("lang") lang: String = "en"): String

    // ai
    @POST("api/v1/ai/explain") suspend fun explain(@Body body: ExplainRequest): ExplainDto
    @POST("api/v1/ai/smart-srt") suspend fun smartSrt(@Body body: SmartSrtRequest): SmartSrtDto
    @POST("api/v1/ai/self-test") suspend fun selfTest(@Body body: SelfTestRequest): SelfTestDto
}
