package com.example.lingoFlix.data.network

import retrofit2.http.GET

/** Public backend endpoints that don't require a logged-in user. */
interface AppApi {
    @GET("api/v1/app/version")
    suspend fun getAppVersion(): AppVersionResponse
}
